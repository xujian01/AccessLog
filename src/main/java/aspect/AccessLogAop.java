package aspect;

import annotation.AccessLog;
import com.alibaba.fastjson.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 访问日志织入，自动添加方法首尾日志
 *
 * @author xujian
 * @create 2019-01-11 10:05
 **/
@Aspect
@Component
public class AccessLogAop {
    @Pointcut(value = "@annotation(com.jiaoyan.common.aspect.AccessLog)")
    public void pointcut() {
    }

    @Around(value = "pointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Class targetClass = pjp.getTarget().getClass();
        String methodName = pjp.getSignature().getName();
        Class<?>[] parameterTypes = ((MethodSignature) pjp.getSignature()).getMethod().getParameterTypes();
        Method targetMethod = targetClass.getDeclaredMethod(methodName,parameterTypes);
        AccessLog accessLog = targetMethod.getAnnotation(AccessLog.class);
        String[] paramsName = accessLog.value();
        Field field = targetClass.getDeclaredField("log");
        field.setAccessible(true);
        Logger logger = (Logger) field.get(targetClass);
        String targetMethodName = pjp.getSignature().getName();
        StringBuilder sb = new StringBuilder();
        Object[] args = pjp.getArgs();
        for (int i=0;i<args.length;i++) {
            Object o = args[i];
            if (o == null) {
                o = "null";
            }
            Object paramValue = o;
            if (o instanceof HttpServletRequest || o instanceof HttpServletResponse) continue;
            //判断是否是基本类型，非基本类型的话转为json字符串
            if (!o.getClass().isPrimitive() && !"java.lang.String".equals(o.getClass().getName())) {
                try {
                    paramValue = JSON.toJSONString(o);
                } catch (Exception e) {
                    paramValue = o.toString();
                }
            }
            sb.append(o.getClass().getSimpleName()+" "+paramsName[i]+" = "+paramValue);
            sb.append(",");
        }
        String beginLog = "----------"+targetMethodName+"("+sb.substring(0,sb.length()-1)+")----------START";
        logger.info(beginLog);
        Object result = pjp.proceed();
        String endLog = "----------"+targetMethodName+"("+sb.substring(0,sb.length()-1)+")----------END";
        logger.info(endLog);
        return result;
    }
}
