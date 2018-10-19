package hpe.com.annotation;

import java.lang.annotation.*;

//作用域方法或者类，在controller 的最前端可能会有url
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestMapping {
    String value();
}
