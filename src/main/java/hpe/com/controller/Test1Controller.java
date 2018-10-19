package hpe.com.controller;

import hpe.com.annotation.MyAutowired;
import hpe.com.annotation.MyController;
import hpe.com.annotation.MyRequestMapping;
import hpe.com.annotation.MyRequestParam;
import hpe.com.service.TestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MyController
@MyRequestMapping("test1")
public class Test1Controller {
    @MyAutowired
    private TestService testService;

    @MyRequestMapping("test")
    public void myTest(HttpServletRequest request, HttpServletResponse response,
                       @MyRequestParam("param") String param){
        try {
            response.getWriter().write( "Test1Controller:the param you send is :"+param);
            testService.printParam(param);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
