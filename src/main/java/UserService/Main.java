package UserService;

import UserService.entity.User;
import UserService.service.UserService;
import UserService.util.HibernateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final UserService userService = new UserService();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        logger.info("Запуск User Service...");

        try {
            showMainMenu();
        } catch (Exception e) {
            logger.error("Критическая ошибка в приложении", e);
            System.err.println("Произошла критическая ошибка: " + e.getMessage());
        } finally {
            HibernateUtil.shutdown();
            scanner.close();
            logger.info("Приложение завершено");
        }
    }

    private static void showMainMenu() {
        boolean running = true;

        while (running) {
            System.out.println("\n=== User Service ===");
            System.out.println("1. Создать пользователя");
            System.out.println("2. Показать всех пользователей");
            System.out.println("3. Найти пользователя по ID");
            System.out.println("4. Найти пользователя по имени");
            System.out.println("5. Обновить пользователя");
            System.out.println("6. Удалить пользователя");
            System.out.println("7. Статистика");
            System.out.println("0. Выход");
            System.out.print("Выберите действие: ");

            String choice = scanner.nextLine();

            try {
                switch (choice) {
                    case "1" -> createUser();
                    case "2" -> showAllUsers();
                    case "3" -> findUserById();
                    case "4" -> findUserByName();
                    case "5" -> updateUser();
                    case "6" -> deleteUser();
                    case "7" -> showStatistics();
                    case "0" -> {
                        running = false;
                        System.out.println("Выход из программы...");
                    }
                    default -> System.out.println("Неверный выбор. Попробуйте снова.");
                }
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
                logger.warn("Ошибка при выполнении операции: {}", e.getMessage());
            }
        }
    }

    private static void createUser() {
        System.out.println("\n--- Создание нового пользователя ---");

        System.out.print("Имя: ");
        String name = scanner.nextLine();

        System.out.print("Email: ");
        String email = scanner.nextLine();

        System.out.print("Возраст: ");
        String ageInput = scanner.nextLine();
        Integer age = ageInput.isEmpty() ? null : Integer.parseInt(ageInput);

        try {
            User user = userService.createUser(name, email, age);
            System.out.println("✅ Пользователь создан успешно!");
            System.out.println("ID: " + user.getId());
        } catch (Exception e) {
            System.out.println("❌ Ошибка: " + e.getMessage());
        }
    }

    private static void showAllUsers() {
        System.out.println("\n--- Список всех пользователей ---");

        List<User> users = userService.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("Пользователей нет");
            return;
        }

        System.out.printf("%-5s %-20s %-30s %-5s %-20s%n",
                "ID", "Имя", "Email", "Возр", "Дата создания");
        System.out.println("-".repeat(85));

        for (User user : users) {
            System.out.printf("%-5d %-20s %-30s %-5d %-20s%n",
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getAge(),
                    user.getCreatedAt());
        }

        System.out.println("Всего: " + users.size() + " пользователей");
    }

    private static void findUserById() {
        System.out.print("\nВведите ID пользователя: ");
        String idInput = scanner.nextLine();

        try {
            Long id = Long.parseLong(idInput);
            userService.getUserById(id).ifPresentOrElse(
                    user -> {
                        System.out.println("\nНайден пользователь:");
                        System.out.println("ID: " + user.getId());
                        System.out.println("Имя: " + user.getName());
                        System.out.println("Email: " + user.getEmail());
                        System.out.println("Возраст: " + user.getAge());
                        System.out.println("Создан: " + user.getCreatedAt());
                    },
                    () -> System.out.println("❌ Пользователь с ID " + id + " не найден")
            );
        } catch (NumberFormatException e) {
            System.out.println("❌ Некорректный формат ID");
        }
    }

    private static void findUserByName() {
        System.out.print("\nВведите имя для поиска: ");
        String name = scanner.nextLine();

        List<User> users = userService.searchUsersByName(name);

        if (users.isEmpty()) {
            System.out.println("Пользователи не найдены");
            return;
        }

        System.out.println("\nНайдено " + users.size() + " пользователей:");
        for (User user : users) {
            System.out.printf("- %s (%s, %d лет)%n",
                    user.getName(), user.getEmail(), user.getAge());
        }
    }

    private static void updateUser() {
        System.out.print("\nВведите ID пользователя для обновления: ");
        String idInput = scanner.nextLine();

        try {
            Long id = Long.parseLong(idInput);

            System.out.print("Новое имя (оставьте пустым, чтобы не менять): ");
            String name = scanner.nextLine();

            System.out.print("Новый email (оставьте пустым, чтобы не менять): ");
            String email = scanner.nextLine();

            System.out.print("Новый возраст (оставьте пустым, чтобы не менять): ");
            String ageInput = scanner.nextLine();
            Integer age = ageInput.isEmpty() ? null : Integer.parseInt(ageInput);

            User updated = userService.updateUser(id,
                    name.isEmpty() ? null : name,
                    email.isEmpty() ? null : email,
                    age);

            System.out.println("✅ Пользователь обновлен успешно!");
            System.out.println("Новые данные: " + updated.getName() + ", " + updated.getEmail());

        } catch (NumberFormatException e) {
            System.out.println("❌ Некорректный формат ID");
        } catch (Exception e) {
            System.out.println("❌ Ошибка: " + e.getMessage());
        }
    }

    private static void deleteUser() {
        System.out.print("\nВведите ID пользователя для удаления: ");
        String idInput = scanner.nextLine();

        try {
            Long id = Long.parseLong(idInput);

            System.out.print("Вы уверены? (y/N): ");
            String confirmation = scanner.nextLine();

            if (confirmation.equalsIgnoreCase("y")) {
                userService.deleteUser(id);
                System.out.println("✅ Пользователь удален успешно!");
            } else {
                System.out.println("Удаление отменено");
            }

        } catch (NumberFormatException e) {
            System.out.println("❌ Некорректный формат ID");
        } catch (Exception e) {
            System.out.println("❌ Ошибка: " + e.getMessage());
        }
    }

    private static void showStatistics() {
        long count = userService.getUserCount();
        System.out.println("\n--- Статистика ---");
        System.out.println("Всего пользователей: " + count);

        // Можно добавить больше статистики
        if (count > 0) {
            System.out.println("\nПоследние 5 пользователей:");
            List<User> users = userService.getAllUsers();
            users.stream()
                    .skip(Math.max(0, users.size() - 5))
                    .forEach(user -> System.out.printf("- %s (%s)%n",
                            user.getName(), user.getCreatedAt()));
        }
    }
}