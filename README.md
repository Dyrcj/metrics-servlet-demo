# metrics-servlet-demo
使用Metrics监控web应用状态的简单测试程序


Metrics提供了一系列用于衡量生产环境中关键组件行为的强大类库，来帮助开发者完成自定义的监控工作。本文通过一个简单的web应用来监控应用内部健康状态。

## 1. 引用Metrics到servlet中
首先将dependency加入到pom文件中：
Metrics需要的包
```xml
<dependency>
    <groupId>io.dropwizard.metrics</groupId>
    <artifactId>metrics-core</artifactId>
    <version>3.2.2</version>
</dependency>
<dependency>
    <groupId>io.dropwizard.metrics</groupId>
    <artifactId>metrics-servlets</artifactId>
    <version>3.2.2</version>
</dependency>
servlet需要的包
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>servlet-api</artifactId>
    <version>3.4</version>
</dependency>
```
spring需要的包
```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>4.3.7.RELEASE</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <version>4.3.7.RELEASE</version>
</dependency>
```
在applicationContext.xml中将spring设置为自动扫描组件，配置如下
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd">
    <context:component-scan base-package="com.yeepay.example"/>
</beans>
```
# 2. 实现数据库健康检查
如果实现Metrics健康检查需要继承他的HealthCheck类：
```java
@Component
public class DatabaseHealthCheck extends HealthCheck {

    public static final String HEALTHCHECK_NAME = "databaseConnection";

    @Autowired
    private Database database;

    @Autowired
    private HealthCheckRegistry healthCheckRegistry;

    @PostConstruct
    public void addToRegistry() {
        healthCheckRegistry.register(HEALTHCHECK_NAME, this);
    }

    protected Result check() throws Exception {
        if (database.isConnected()) {
            return HealthCheck.Result.healthy();
        } else {
            return HealthCheck.Result.unhealthy("Cannot connect to " + database.getUrl());
        }
    }
}
```
上面代码中的Database表示待监控的类，通过判断它是否为连接状态就可以知道它是否健康，同时将DatabaseHealthCheck注册到HealthCheckRegistry中。
# 3. 实现ServletContextListener
通过实现HealthCheckServlet.ContextListener类，可以将之前提到的HealthCheckRegistry传递给ServletContext。
```java
public class HealthCheckServletContextListener extends HealthCheckServlet.ContextListener {
    private final HealthCheckRegistry registry;

    public HealthCheckServletContextListener(HealthCheckRegistry metricRegistry) {
        this.registry = metricRegistry;
    }

    protected ExecutorService getExecutorService() {
        return Executors.newCachedThreadPool();
    }

    protected HealthCheckRegistry getHealthCheckRegistry() {
        return registry;
    }
}
```
同时可以设置HealthCheck的线程池，如果不设置的话就是在servlet线程中进行单线程的健康检查。Metrics源码如下：
```java
/**
 * Runs the registered health checks and returns a map of the results.
 *
 * @return a map of the health check results
 */
public SortedMap<String, HealthCheck.Result> runHealthChecks() {
    final SortedMap<String, HealthCheck.Result> results = new TreeMap<String, HealthCheck.Result>();
    for (Map.Entry<String, HealthCheck> entry : healthChecks.entrySet()) {
        final Result result = entry.getValue().execute();
        results.put(entry.getKey(), result);
    }
    return Collections.unmodifiableSortedMap(results);
}

/**
 * Runs the registered health checks in parallel and returns a map of the results.
 *
 * @param executor object to launch and track health checks progress
 * @return a map of the health check results
 */
public SortedMap<String, HealthCheck.Result> runHealthChecks(ExecutorService executor) {
    final Map<String, Future<HealthCheck.Result>> futures = new HashMap<String, Future<Result>>();
    for (final Map.Entry<String, HealthCheck> entry : healthChecks.entrySet()) {
        futures.put(entry.getKey(), executor.submit(new Callable<Result>() {
            @Override
            public Result call() throws Exception {
                return entry.getValue().execute();
            }
        }));
    }

    final SortedMap<String, HealthCheck.Result> results = new TreeMap<String, HealthCheck.Result>();
    for (Map.Entry<String, Future<Result>> entry : futures.entrySet()) {
        try {
            results.put(entry.getKey(), entry.getValue().get());
        } catch (Exception e) {
            LOGGER.warn("Error executing health check {}", entry.getKey(), e);
            results.put(entry.getKey(), HealthCheck.Result.unhealthy(e));
        }
    }

    return Collections.unmodifiableSortedMap(results);
}
```
可以看出如果设置了线程池就会并行探测结果。

# 4. 配置MetricsConfiguration配置文件
```java
@Configuration
public class MetricsConfiguration {
    @Bean
    public HealthCheckRegistry newHealthCheckRegistry() {
        return new HealthCheckRegistry();
    }

    @Bean
    public MetricRegistry newMetricRegistry() {
        return new MetricRegistry();
    }
}
```
在Configuration文件中新建registry，自动装配。
# 5. 实现自己的ServletContextListener将registry装配到Listener中：
```java
public class MetricsServletsWiringContextListener implements ServletContextListener {
    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    private HealthCheckRegistry healthCheckRegistry;

    private MetricsServletContextListener metricsServletContextListener;
    private HealthCheckServletContextListener healthCheckServletContextListener;
    public void contextInitialized(ServletContextEvent event) {
        WebApplicationContextUtils.getRequiredWebApplicationContext(event.getServletContext())
                .getAutowireCapableBeanFactory()
                .autowireBean(this);

        metricsServletContextListener = new MetricsServletContextListener(metricRegistry);
        healthCheckServletContextListener = new HealthCheckServletContextListener(healthCheckRegistry);

        metricsServletContextListener.contextInitialized(event);
        healthCheckServletContextListener.contextInitialized(event);
    }

    public void contextDestroyed(ServletContextEvent event) {

    }
}
```
初始化context时的第一件事就是将listener自己装配到springcontext中：
```java
WebApplicationContextUtils.getRequiredWebApplicationContext(event.getServletContext())
        .getAutowireCapableBeanFactory()
        .autowireBean(this);
```
之后就是调用healthCheckServletContextListener的初始化方法，通过源码可看出他将healthcheckregistry与healthcheckexecutor主持到了ServletContext中：
（Metrics源码）
```java
@Override
public void contextInitialized(ServletContextEvent event) {
    final ServletContext context = event.getServletContext();
    context.setAttribute(HEALTH_CHECK_REGISTRY, getHealthCheckRegistry());
    context.setAttribute(HEALTH_CHECK_EXECUTOR, getExecutorService());
}
```
之后在运行时从ServletContext取出
（Metrics源码）
```java
@Override
public void init(ServletConfig config) throws ServletException {
    super.init(config);

    if (null == registry) {
        final Object registryAttr = config.getServletContext().getAttribute(HEALTH_CHECK_REGISTRY);
        if (registryAttr instanceof HealthCheckRegistry) {
            this.registry = (HealthCheckRegistry) registryAttr;
        } else {
            throw new ServletException("Couldn't find a HealthCheckRegistry instance.");
        }
    }

    final Object executorAttr = config.getServletContext().getAttribute(HEALTH_CHECK_EXECUTOR);
    if (executorAttr instanceof ExecutorService) {
        this.executorService = (ExecutorService) executorAttr;
    }

    this.mapper = new ObjectMapper().registerModule(new HealthCheckModule());
}
```

每次get请求的时候计算一次健康状态：
（Metrics源码）
```java
@Override
protected void doGet(HttpServletRequest req,
                     HttpServletResponse resp) throws ServletException, IOException {
    final SortedMap<String, HealthCheck.Result> results = runHealthChecks();
    resp.setContentType(CONTENT_TYPE);
    resp.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
    if (results.isEmpty()) {
        resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
    } else {
        if (isAllHealthy(results)) {
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    final OutputStream output = resp.getOutputStream();
    try {
        getWriter(req).writeValue(output, results);
    } finally {
        output.close();
    }
}
```
# 6. 最后就是设置web.xml属性了：
```xml
<web-app>
    <display-name>Archetype Created Web Application</display-name>
    <listener>
        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
    </listener>
    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>/WEB-INF/applicationContext.xml</param-value>
    </context-param>
    <listener>
        <listener-class>com.yeepay.example.MetricsServletsWiringContextListener</listener-class>
    </listener>
    <servlet>
        <servlet-name>metrics</servlet-name>
        <servlet-class>com.codahale.metrics.servlets.AdminServlet</servlet-class>
    </servlet>
    <servlet-mapping>
        <servlet-name>metrics</servlet-name>
        <url-pattern>/metrics/*</url-pattern>
    </servlet-mapping>
</web-app>
```
启动tomcat，通过访问metrics就可以看到之前设置的数据库连接状态：没有图片
