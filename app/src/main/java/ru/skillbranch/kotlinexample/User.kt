package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {

    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.normalizeRawPhone() //replace("[^+\\d]".toRegex(), "")
        }

    private var _login: String? = null
    var login: String
        set(value) {
            _login = value.toLowerCase()
        }
        get() = _login!!

    private var _salt: String? = null
    private val salt: String by lazy {
        _salt ?: ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
    }

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    //for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary phone constructor")
        passwordHash = encrypt(password)
    }

    //for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(rawPhone, code)
    }

    //for csv
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        hashPassword: String?,
        rawPhone: String?
    ) : this(
        firstName,
        lastName,
        email = email,
        rawPhone = rawPhone,
        meta = mapOf("src" to "csv")
    ) {
        if (email != null ) {
            hashPassword?.let {
                with(hashPassword.toList(":")) {
                    if (size != 2) {
                        throw IllegalArgumentException("Salt and hash of password must be specify")
                    }
                    _salt = first()
                    passwordHash = last()
                }
            } ?: throw IllegalArgumentException("Salt and hash of password must be specify")
        }
        else if (rawPhone != null) {
            // для импорта пользователей с телефоном, генерируем новый код и отсылаем им
            val code = generateAccessCode()
            passwordHash = encrypt(code)
            accessCode = code
            sendAccessCodeToUser(rawPhone, code)
        }
    }

    init {
        println("First init block, primary constructor was called")

        check(!firstName.isBlank()) { "FirstName must be not blank" }
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) { "Email or phone must be not blank" }

        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass))
            passwordHash = encrypt(newPass)
        else
            throw IllegalArgumentException("The entered password does not match the current password")
    }

    fun changeAccessCode() {
        phone?.let {
            generateAccessCode()
            val code = generateAccessCode()
            passwordHash = encrypt(code)
            accessCode = code
            sendAccessCodeToUser(it, code)
        }
    }

    private fun encrypt(password: String): String = salt.plus(password).md5() //good

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun sendAccessCodeToUser(phone: String, code: String) {
        println("..... sending access code: $code on $phone")
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) //16 byte
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory {

        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null,
            isFromCsv: Boolean = false
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()
            return when {
                !phone.isNullOrBlank() && !phone.isPhone() ->
                    throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
                isFromCsv -> User(firstName, lastName, email = email, hashPassword = password, rawPhone = phone)
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email,
                    password
                )
                else -> throw IllegalArgumentException("Email or phone must be not null or blank")
            }
        }

        private fun String.toList(delimiter: String): List<String> {
            return this.split(delimiter)
                .filter { it.isNotBlank() }
        }

        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.toList(" ")
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException(
                            "Fullname must contain only first name " +
                                    "and last name, current split result ${this@fullNameToPair}"
                        )
                    }
                }
        }

        fun normalizePhone(rawPhone: String): String {
            return if (rawPhone.isPhone()) rawPhone.normalizeRawPhone() else ""
        }

        private fun String.normalizeRawPhone(): String {
            return replace("[^+\\d]".toRegex(), "")
        }

        private fun String.isPhone(): Boolean {
            return replace("[+ \\-()\\d]".toRegex(), "").equals("")
                    && normalizeRawPhone().length == 12
        }
    }

}