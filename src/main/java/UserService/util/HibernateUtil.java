package UserService.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HibernateUtil {
    private static final Logger logger = LoggerFactory.getLogger(HibernateUtil.class);
    private static SessionFactory sessionFactory;

    static {
        try {
            sessionFactory = new Configuration()
                    .configure("hibernate.cfg.xml")
                    .buildSessionFactory();
            logger.info("SessionFactory создана успешно");
        } catch ( Throwable exception){
            logger.error("Ошибка при создании SessionFactory",exception);
            throw new ExceptionInInitializerError(exception);
        }
    }
    public static SessionFactory getSessionFactory(){
        return sessionFactory;
    }
    public static void shutdown(){
        if(sessionFactory != null){
            sessionFactory.close();
            logger.info("SessionFactory закрыта");
        }
    }
}
