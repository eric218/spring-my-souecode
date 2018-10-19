package hpe.com;

import hpe.com.annotation.MyAutowired;
import hpe.com.annotation.MyController;
import hpe.com.annotation.MyRequestMapping;
import hpe.com.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class MyDispatcherServlet extends HttpServlet {


    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    //handlerMapping的类型可以自定义为Handler
    private Map<String, Method> handlerMapping = new  HashMap<>();

    private Map<String, Object> controllerMap  =new HashMap<>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 注释掉父类实现，不然会报错：405 HTTP method GET is not supported by this URL
        //super.doPost(req, resp);
        System.out.println("执行MyDispatcherServlet的doPost()");
        try {
            //处理请求
            doDispatch(req,resp);
        } catch (Exception e) {
            resp.getWriter().write("500!! Server Exception");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 注释掉父类实现，不然会报错：405 HTTP method GET is not supported by this URL
        //super.doPost(req, resp);
        System.out.println("执行MyDispatcherServlet的doPost()");
        try {
            //处理请求
            doDispatch(req,resp);
        } catch (Exception e) {
            resp.getWriter().write("500!! Server Exception");
        }

    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2.初始化所有相关联的类,扫描用户设定的包下面所有的类
        doScanner(properties.getProperty("scanPackage"));
        //3.拿到扫描到的类,通过反射机制,实例化,并且放到ioc容器中(k-v  beanName-bean) beanName默认是首字母小写
        doInstance();
        // 4.自动化注入依赖，
        //doAutowired();

        //5.初始化HandlerMapping(将url和method对应上)
        initHandlerMapping();


        // 4.自动化注入依赖 //将service的实例注入到 controller 的 service里
        doAutowired2();

    }



    private void  doLoadConfig(String location){
        //把web.xml中的contextConfigLocation对应value值的文件加载到流里面
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            //用Properties文件加载文件里的内容
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            //关流
            if(null!=resourceAsStream){
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    private void doScanner(String packageName) {
        //把所有的.替换成/
        URL url  =this.getClass().getClassLoader().getResource("/"+packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if(file.isDirectory()){
                //递归读取包
                doScanner(packageName+"."+file.getName());
            }else{
                String className =packageName +"." +file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }


    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            try {

                Class<?> clazz =Class.forName(className);

                //实例化@MyController
                if(clazz.isAnnotationPresent(MyController.class)){
                    ioc.put(clazz.getSimpleName(),clazz.newInstance());
                }

                //实例化@MyService
                else if(clazz.isAnnotationPresent(MyService.class)){
                    MyService myService=clazz.getAnnotation(MyService.class);
                    String beanName=myService.value();
                    if ("".equals(beanName.trim())){
                        beanName=toLowerFirstWord(clazz.getSimpleName());
                    }

                    Object instance= clazz.newInstance();
                    ioc.put(beanName,instance);
                    Class[] interfaces=clazz.getInterfaces();
                    for (Class<?> i:interfaces){
                        ioc.put(i.getName(),instance);
                    }
                }
                else{
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private void doAutowired(){

        if (ioc.isEmpty()){
            return;
        }
        for (Map.Entry<String,Object> entry:ioc.entrySet()){
            //包括私有的方法，在spring中没有隐私，@MyAutowired可以注入public、private字段
            Field[] fields=entry.getValue().getClass().getDeclaredFields();
            for (Field field:fields){
                if (!field.isAnnotationPresent(MyAutowired.class)){
                    continue;
                }
                MyAutowired autowired= field.getAnnotation(MyAutowired.class);
                String beanName=autowired.value().trim();
                if ("".equals(beanName)){
                    beanName=field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    //将service的实例注入到 controller 的 service里
                    field.set(entry.getValue(),ioc.get(beanName));
                }catch (Exception e){
                    e.printStackTrace();
                    continue;
                }

            }
        }
    }

    private String toLowerFirstWord(String name){

        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

    private void initHandlerMapping(){

        if(ioc.isEmpty()){
            return;
        }
        try {
            for (Map.Entry<String, Object> entry: ioc.entrySet()) {
                Class<? extends Object> clazz = entry.getValue().getClass();
                if(!clazz.isAnnotationPresent(MyController.class)){
                    continue;
                }

                //拼url时,是controller头的url拼上方法上的url
                String baseUrl ="";
                if(clazz.isAnnotationPresent(MyRequestMapping.class)){
                    MyRequestMapping annotation = clazz.getAnnotation(MyRequestMapping.class);
                    baseUrl=annotation.value();
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if(!method.isAnnotationPresent(MyRequestMapping.class)){
                        continue;
                    }
                    MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
                    String url = annotation.value();

                    url =(baseUrl+"/"+url).replaceAll("/+", "/");
                    handlerMapping.put(url,method);
                    controllerMap.put(url,clazz.newInstance());
                    System.out.println(url+","+method);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if(handlerMapping.isEmpty()){
            return;
        }
        String url =req.getRequestURI();
        String contextPath = req.getContextPath();
        url=url.replace(contextPath, "").replaceAll("/+", "/");
        // 去掉url前面的斜杠"/"，所有的@MyRequestMapping可以不用写斜杠"/"
        if(url.lastIndexOf('/')!=0){
            url=url.substring(1);
        }
        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 NOT FOUND!");
            System.out.println("404 NOT FOUND!");
            return;
        }
        Method method =this.handlerMapping.get(url);
        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        //获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();
        //保存参数值
        Object [] paramValues= new Object[parameterTypes.length];
        //方法的参数列表
        for (int i = 0; i<parameterTypes.length; i++){
            //根据参数名称，做某些处理
            String requestParam = parameterTypes[i].getSimpleName();
            if (requestParam.equals("HttpServletRequest")){
                //参数类型已明确，这边强转类型
                paramValues[i]=req;
                continue;
            }
            if (requestParam.equals("HttpServletResponse")){
                paramValues[i]=resp;
                continue;
            }
            if(requestParam.equals("String")){
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value =Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                    paramValues[i]=value;
                }
            }
        }
        //利用反射机制来调用
        try {
            //第一个参数是method所对应的实例 在ioc容器中
            //method.invoke(this.controllerMap.get(url), paramValues);
            method.invoke(this.controllerMap.get(url), paramValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void doAutowired2(){

        if (controllerMap.isEmpty()){
            return;
        }
        for (Map.Entry<String,Object> entry:controllerMap.entrySet()){
            //包括私有的方法，在spring中没有隐私，@MyAutowired可以注入public、private字段
            Field[] fields=entry.getValue().getClass().getDeclaredFields();
            for (Field field:fields){
                if (!field.isAnnotationPresent(MyAutowired.class)){
                    continue;
                }
                MyAutowired autowired= field.getAnnotation(MyAutowired.class);
                String beanName=autowired.value().trim();
                if ("".equals(beanName)){
                    beanName=field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    //将service的实例注入到 controller 的 service里
                    field.set(entry.getValue(),ioc.get(beanName));
                }catch (Exception e){
                    e.printStackTrace();
                    continue;
                }

            }
        }
    }




}
