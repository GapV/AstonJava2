package UserService.dao;

import UserService.entity.User;
import UserService.util.HibernateUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class UserDaoImpl implements UserDao {

    private static final Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);

    @Override
    public User save(User user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(user);
            transaction.commit();
            logger.info("Пользователь сохранен: {}", user.getEmail());
            return user;

        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Ошибка при сохранении пользователя", e);
            throw new RuntimeException("Не удалось сохранить пользователя", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = session.get(User.class, id);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            logger.error("Ошибка при поиске пользователя по ID: {}", id, e);
            throw new RuntimeException("Ошибка при поиске пользователя", e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery(
                    "FROM User WHERE email = :email", User.class);
            query.setParameter("email", email);

            return query.uniqueResultOptional();
        } catch (Exception e) {
            logger.error("Ошибка при поиске пользователя по email: {}", email, e);
            throw new RuntimeException("Ошибка при поиске пользователя", e);
        }
    }

    @Override
    public List<User> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery("FROM User", User.class);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Ошибка при получении всех пользователей", e);
            throw new RuntimeException("Ошибка при получении пользователей", e);
        }
    }

    @Override
    public List<User> findByName(String name) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery(
                    "FROM User WHERE name LIKE :name", User.class);
            query.setParameter("name", "%" + name + "%");

            return query.getResultList();
        } catch (Exception e) {
            logger.error("Ошибка при поиске пользователей по имени: {}", name, e);
            throw new RuntimeException("Ошибка при поиске пользователей", e);
        }
    }

    @Override
    public List<User> findByAgeGreaterThan(int age) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery(
                    "FROM User WHERE age > :age ORDER BY age DESC", User.class);
            query.setParameter("age", age);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Ошибка при поиске пользователей старше {}", age, e);
            throw new RuntimeException("Ошибка при поиске пользователей", e);
        }
    }

    @Override
    public User update(User user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            User updatedUser = session.merge(user);
            transaction.commit();
            logger.info("Пользователь обновлен: {}", user.getEmail());
            return updatedUser;

        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Ошибка при обновлении пользователя: {}", user.getId(), e);
            throw new RuntimeException("Не удалось обновить пользователя", e);
        }
    }

    @Override
    public void delete(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            User user = session.get(User.class, id);
            if (user != null) {
                session.remove(user);
                logger.info("Пользователь удален: {}", id);
            } else {
                logger.warn("Пользователь с ID {} не найден", id);
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Ошибка при удалении пользователя: {}", id, e);
            throw new RuntimeException("Не удалось удалить пользователя", e);
        }
    }

    @Override
    public void delete(User user) {
        delete(user.getId());
    }

    @Override
    public long count() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(*) FROM User", Long.class);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Ошибка при подсчете пользователей", e);
            throw new RuntimeException("Ошибка при подсчете пользователей", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(*) FROM User WHERE email = :email", Long.class);
            query.setParameter("email", email);

            return query.uniqueResult() > 0;
        } catch (Exception e) {
            logger.error("Ошибка при проверке email: {}", email, e);
            throw new RuntimeException("Ошибка при проверке пользователя", e);
        }
    }
}
