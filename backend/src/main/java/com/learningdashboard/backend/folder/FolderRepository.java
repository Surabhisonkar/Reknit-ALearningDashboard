package com.learningdashboard.backend.folder;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Name comparisons here are plain equality on purpose: the {@code name}
 * column's collation (utf8mb4_0900_as_ci) already makes them case-insensitive
 * and accent-sensitive, exactly like the unique key. An {@code IgnoreCase}
 * finder would wrap the column in LOWER() and skip that index.
 */
public interface FolderRepository extends JpaRepository<Folder, UUID> {

    List<Folder> findByUserIdOrderByNameAsc(UUID userId);

    Optional<Folder> findByIdAndUserId(UUID id, UUID userId);

    Optional<Folder> findByUserIdAndName(UUID userId, String name);

    boolean existsByUserIdAndName(UUID userId, String name);

    boolean existsByUserIdAndNameAndIdNot(UUID userId, String name, UUID id);

    /** Auto-filing's read: takes a row lock (SELECT ... FOR UPDATE), so it also sees a folder a concurrent save just committed. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Folder f where f.userId = :userId and f.name = :name")
    Optional<Folder> lockByUserIdAndName(@Param("userId") UUID userId, @Param("name") String name);

    /**
     * Creates the folder unless one with the same (case-insensitive) name
     * already exists for this user - then it's a no-op instead of a
     * duplicate-key error, which keeps the caller's transaction usable.
     * Ids are passed as strings because the columns are CHAR(36).
     */
    @Modifying
    @Query(value = "INSERT INTO folders (id, user_id, name, color, created_at, updated_at) "
            + "VALUES (:id, :userId, :name, :color, :now, :now) "
            + "ON DUPLICATE KEY UPDATE id = id", nativeQuery = true)
    int insertIfAbsent(@Param("id") String id, @Param("userId") String userId, @Param("name") String name,
                       @Param("color") String color, @Param("now") Instant now);

    /** One entry per folder - the least-used colour picker counts them. */
    @Query("select f.color from Folder f where f.userId = :userId")
    List<FolderColor> findColorsByUserId(@Param("userId") UUID userId);
}
