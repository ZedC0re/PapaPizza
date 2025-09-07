package papapizza.app.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class HeadCrosscutAspect {
	private final Logger logger = LoggerFactory.getLogger(HeadCrosscutAspect.class);

	@Around(value = "@annotation(papapizza.app.aop.GivePapaHead)")
	public Object givePapaHead(ProceedingJoinPoint joinPoint) throws Throwable{
		String headless = System.getProperty("java.awt.headless");
		logger.warn("Setting headless false");
		System.setProperty("java.awt.headless", "false");
		long t_start = System.currentTimeMillis();
		Object result = joinPoint.proceed();
		logger.warn("method time:"+(System.currentTimeMillis()-t_start)+"ms");
		logger.warn("Resetting headless to its previous value:"+headless);
		System.setProperty("java.awt.headless", headless);
		return result;
	}
}
