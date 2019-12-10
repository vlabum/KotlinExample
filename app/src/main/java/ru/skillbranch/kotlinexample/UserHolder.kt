package ru.skillbranch.kotlinexample

import java.lang.IllegalArgumentException

object UserHolder {

    private val map = mutableMapOf<String, User>()

    fun clearMap() {
        map.clear()
    }

    /*
    * Реализуй метод registerUser(fullName: String, email: String, password: String) возвращающий объект User,
    * если пользователь с таким же логином уже есть в системе необходимо бросить
    * исключение IllegalArgumentException("A user with this email already exists")
    * */
    fun registerUser(
        fullName: String, email: String, password: String
    ): User {
        return User.makeUser(fullName, email = email, password = password)
            .apply {
                if (map[login] != null) {
                    throw IllegalArgumentException("A user with this email already exists")
                }
            }
            .also { user ->
                map[user.login.trim()] = user
            }
    }

    /*
    * Реализуй метод registerUserByPhone(fullName: String, rawPhone: String) возвращающий
    * объект User (объект User должен содержать поле accessCode с 6 значным значением состоящим из случайных
    * строчных и прописных букв латинского алфавита и цифр от 0 до 9), если пользователь с таким же телефоном
    * уже есть в системе необходимо бросить ошибку IllegalArgumentException("A user with this phone already exists")
    * валидным является любой номер телефона содержащий первым символом + и 11 цифр и не содержащий буквы, иначе необходимо бросить
    * исключение IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
    * */
    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        return User.makeUser(fullName, phone = rawPhone)
            .apply {
                if (map[login] != null) {
                    throw IllegalArgumentException("A user with this phone already exists")
                }
            }
            .also { user ->
                map[user.login.trim()] = user
            }
    }

    /**
     * Реализуй метод loginUser(login: String, password: String) : String возвращающий
     * поле userInfo пользователя с соответствующим логином и паролем (логин для пользователя phone или email,
     * пароль соответственно accessCode или password указанный при регистрации методом registerUser)
     * или возвращающий null если пользователь с указанным логином и паролем не найден (или неверный пароль)
     *
     */
    fun loginUser(
        login: String, password: String
    ): String? {
        return map[login.trim()]?.run {
            if (checkPassword(password)) this.userInfo
            else null
        } ?: map[User.normalizePhone(login)]?.run {
            if (checkPassword(password)) this.userInfo
            else null
        }
    }

    /**
     * Реализуй метод requestAccessCode(login: String) : Unit, после выполнения данного метода у пользователя
     * с соответствующим логином должен быть сгенерирован новый код авторизации и помещен в свойство accessCode,
     * соответственно должен измениться и хеш пароля пользователя (вызов метода loginUser должен отрабатывать корректно)
     */
    fun requestAccessCode(login: String): Unit {
        map[User.normalizePhone(login)]?.changeAccessCode()
    }

    /**
     * Реализуй метод importUsers(list: List): List, в качестве аргумента принимает список строк где разделителем полей является ";"
     * (Пример: " John Doe ;JohnDoe@unknow.com;[B@7591083d:c6adb4becdc64e92857e1e2a0fd6af84;;")
     * метод должен вернуть коллекцию список User (Пример возвращаемого userInfo:
     * firstName: John
     * lastName: Doe
     * login: johndoe@unknow.com
     * fullName: John Doe
     * initials: J D
     * email: JohnDoe@unknow.com
     * phone: null
     * meta: {src=csv}
     * ), при этом meta должно содержать "src" : "csv", если сзначение в csv строке пустое то
     * соответствующее свойство в объекте User должно быть null, обратите внимание что salt и hash
     * пароля в csv разделены ":", после импорта пользователей вызов метода loginUser должен отрабатывать
     * корректно (достаточно по логину паролю)
     */
    fun importUsers(list: List<String>): List<User> {
        val listUser = mutableListOf<User>()
        for (item in list) {
            val listItem = list.first().split(";").map { it.trim().ifEmpty { null } }
            listItem[0]?.let {
                listUser.add(
                    User.makeUser(listItem[0]!!, listItem[1], listItem[2], listItem[3], isFromCsv = true)
                        .apply {
                            if (map[login] != null) {
                                throw IllegalArgumentException("A user with this email already exists")
                            }
                        }
                        .also { user ->
                            map[user.login.trim()] = user
                        }
                )
            }
        }
        return listUser
    }

}