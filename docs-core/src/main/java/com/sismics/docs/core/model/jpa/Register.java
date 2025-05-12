package com.example.model.jpa; // 请根据您的项目结构更改包名

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.google.common.base.MoreObjects; // 假设您仍希望使用 Guava 的 MoreObjects

/**
 * Register entity. // Javadoc 注释也更新了
 */
@Entity // 默认情况下，JPA 实体名将是类名 "Register"
@Table(name = "register") // 表名仍为 "register"
public class Register { // <--- 类名已更改为 Register

    /**
     * User ID (Primary Key).
     * 实体 Register 的 ID，通常建议与实体本身相关，这里暂时保留 USER_ID_C
     * 如果希望更通用，可以改为 REG_ID_C 或简单的 ID_C
     */
    @Id
    @Column(name = "REG_ID_C", length = 36) // 列名可以考虑更新以反映实体名，例如 REG_ID_C
    private String id;

    /**
     * Username.
     */
    @Column(name = "REG_USERNAME_C", nullable = false, unique = true, length = 50) // 列名更新以反映实体名
    private String username;

    /**
     * User email.
     */
    @Column(name = "REG_EMAIL_C", nullable = false, unique = true, length = 100) // 列名更新以反映实体名
    private String email;

    /**
     * Flag indicating if the user has completed registration.
     * 标记用户是否已通过注册。
     */
    @Column(name = "REG_IS_CONFIRMED_B", nullable = false) // 列名更新并调整了语义以匹配 "Register" 实体
    private boolean confirmed; // 字段名可以改为 'confirmed' 或 'isConfirmed' 来表示注册已确认

    // Default constructor (JPA 要求)
    public Register() { // <--- 构造函数名已更改
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isConfirmed() { // Getter 方法名也相应调整
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) { // Setter 方法名和参数名也相应调整
        this.confirmed = confirmed;
    }

    @Override
    public String toString() {
        // MoreObjects.toStringHelper(this) 会自动使用当前的类名 "Register"
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("username", username)
                .add("email", email)
                .add("confirmed", confirmed) // 字段名更新
                .toString();
    }
}