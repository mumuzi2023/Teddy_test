package com.sismics.docs.core.dao; // 保持与示例一致的包路径，您可以根据需要调整

import com.example.model.jpa.Register; // 导入 Register 实体
import com.sismics.util.context.ThreadLocalContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.UUID; // 用于生成 ID (如果需要)

/**
 * Data Access Object for Register entities.
 * 提供对 Register 实体的 CRUD 操作以及特定的查询方法。
 */
public class RegisterDao {

    /**
     * Persists a new Register entity directly.
     * 实体应已设置其字段。ID 应该在传入前设置，或者在 Register 构造函数中处理。
     *
     * @param registration The Register entity to create.
     */
    public void create(Register registration) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        em.persist(registration);
    }

    /**
     * Creates and persists a new registration entry from individual parameters.
     * The 'confirmed' status will be set to false by default.
     * An ID will be automatically generated.
     *
     * @param username The username for the registration.
     * @param email    The email for the registration.
     * @return The newly created and persisted Register entity.
     */
    public Register createRegistration(String username, String email) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();

        Register newRegistration = new Register();
        newRegistration.setId(UUID.randomUUID().toString()); // 自动生成 UUID 作为 ID
        newRegistration.setUsername(username);
        newRegistration.setEmail(email);
        newRegistration.setConfirmed(false); // 默认设置为未通过/未确认

        em.persist(newRegistration);
        return newRegistration;
    }

    /**
     * Finds a Register entity by its ID.
     *
     * @param id The ID of the Register entity.
     * @return The found Register entity, or null if not found.
     */
    public Register findById(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        return em.find(Register.class, id);
    }

    /**
     * Marks a registration entry as confirmed (passed).
     * It finds the registration by ID and sets its 'confirmed' status to true.
     *
     * @param id The ID of the registration entry to confirm.
     * @return The updated Register entity if found and updated, otherwise null.
     */
    public Register confirmRegistrationById(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Register registration = em.find(Register.class, id);

        if (registration != null) {
            registration.setConfirmed(true);
            return em.merge(registration); // merge() 会将更改持久化并返回受管实体
        }
        return null; // 如果未找到对应 ID 的条目
    }

    /**
     * Retrieves all registration entries from the database.
     *
     * @return A list of all Register entities.
     */
    public List<Register> findAll() {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        String jpql = "SELECT r FROM Register r ORDER BY r.username ASC"; // 您可以根据需要调整排序
        TypedQuery<Register> query = em.createQuery(jpql, Register.class);
        return query.getResultList();
    }

    /**
     * Updates an existing Register entity.
     * (提供一个通用的更新方法，类似于 ChatMessageDao)
     *
     * @param registration The Register entity with updated information.
     * @return The updated, managed Register entity.
     */
    public Register update(Register registration) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        return em.merge(registration);
    }

    /**
     * Deletes a Register entity from the database.
     * (提供一个通用的删除方法，类似于 ChatMessageDao)
     *
     * @param registration The Register entity to delete.
     */
    public void delete(Register registration) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        if (registration == null || registration.getId() == null) {
            return;
        }
        // 确保实体是受管的
        if (em.contains(registration)) {
            em.remove(registration);
        } else {
            Register managedRegistration = findById(registration.getId());
            if (managedRegistration != null) {
                em.remove(managedRegistration);
            }
        }
    }

    /**
     * Deletes a Register entity by its ID.
     * (提供一个通用的按ID删除方法，类似于 ChatMessageDao)
     *
     * @param id The ID of the Register entity to delete.
     */
    public void deleteById(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Register registration = findById(id);
        if (registration != null) {
            em.remove(registration);
        }
    }
}