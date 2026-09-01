package com.phoenix.phoenixnet.db

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles user authentication and registration logic.
 * Bridges Firebase (Auth/Firestore) with local Room storage.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /**
     * Registers a new user both on Firebase and locally.
     */
    suspend fun register(
        firstName: String,
        middleName: String,
        lastName: String,
        triggerWord: String
    ): Result<UserEntity> {
        val email = generatePseudoEmail(firstName, lastName)
        val user = UserEntity(
            userId = java.util.UUID.randomUUID().toString(), // Temporary local ID
            email = email,
            firstName = firstName,
            middleName = middleName,
            lastName = lastName,
            triggerWordHash = hashTriggerWord(triggerWord)
        )

        return try {
            // 1. Attempt Firebase Auth (Cloud Identity)
            // Firebase requires at least 6 characters for password. Pad if necessary.
            val firebasePassword = if (triggerWord.length < 6) "${triggerWord}_mesh" else triggerWord
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, firebasePassword).await()
            val cloudUserId = authResult.user?.uid ?: user.userId
            
            val cloudUser = user.copy(userId = cloudUserId)

            // 2. Store profile in Firestore
            firestore.collection("users").document(cloudUserId).set(cloudUser).await()

            // 3. Save locally in Room
            userDao.insertOrUpdateUser(cloudUser)
            Result.success(cloudUser)
        } catch (e: Exception) {
            // OFFLINE FALLBACK: Save locally anyway so the user can access the dashboard
            userDao.insertOrUpdateUser(user)
            // Return success with a warning or just proceed to the dashboard
            Result.success(user) 
        }
    }

    /**
     * Attempts online login with offline fallback.
     */
    suspend fun login(
        firstName: String,
        middleName: String,
        lastName: String,
        triggerWord: String
    ): Result<UserEntity> {
        val email = generatePseudoEmail(firstName, lastName)
        
        return try {
            // Attempt Online Login
            val firebasePassword = if (triggerWord.length < 6) "${triggerWord}_mesh" else triggerWord
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, firebasePassword).await()
            val userId = authResult.user?.uid ?: throw Exception("Login failed")

            // Fetch latest profile from Firestore
            val doc = firestore.collection("users").document(userId).get().await()
            val user = doc.toObject(UserEntity::class.java) ?: throw Exception("Profile not found")

            // Sync with local Room
            userDao.insertOrUpdateUser(user)
            
            Result.success(user)
        } catch (e: Exception) {
            // Offline Fallback: Verify against local Room
            val hash = hashTriggerWord(triggerWord)
            val localUser = userDao.verifyUser(firstName, middleName, lastName, hash)
            if (localUser != null) {
                Result.success(localUser)
            } else {
                Result.failure(Exception("Offline login failed. Ensure you have registered on this device."))
            }
        }
    }

    /**
     * Retrieves the current user session.
     */
    fun getActiveSession(): UserEntity? {
        return userDao.getActiveUser()
    }

    /**
     * Clears the current user session.
     */
    suspend fun logout() {
        firebaseAuth.signOut()
        userDao.clearAllUsers()
    }

    private fun generatePseudoEmail(first: String, last: String): String {
        return "${first.lowercase()}.${last.lowercase()}@phoenixnet.mesh"
    }

    private fun hashTriggerWord(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
