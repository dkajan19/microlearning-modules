package com.example.myapplication

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ApiService {
    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun login(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("username") username: String,
        @Field("password") password: String,
    ): LoginResponse

    @GET("get-full-user-parameters")
    suspend fun getFullUserParameters(
        @Header("Authorization") authHeader: String
    ): UserDetailsResponse

    @GET("get-active-user-courses2")
    suspend fun getActiveUserCourses(
        @Header("Authorization") authHeader: String
    ): CoursesResponse

    @POST("get-leaders2")
    suspend fun getLeaders(
        @Header("Authorization") authHeader: String,
        @Body body: JsonObject
    ): LeadersResponse

    @GET("get-profile-data")
    suspend fun getProfileData(
        @Header("Authorization") authHeader: String
    ): ProfileResponse

    @GET("get-registration-data")
    suspend fun getRegistrationData(
        @Header("Authorization") authHeader: String
    ): RegistrationDataResponse

    @POST("profile-change")
    suspend fun profileChange(
        @Header("Authorization") authHeader: String,
        @Body registrationRequest: RegistrationRequest
    ): Unit

    @GET("get-active-chapters2/{courseId}")
    suspend fun getCourseChapters(
        @Header("Authorization") authHeader: String,
        @retrofit2.http.Path("courseId") courseId: Int
    ): CourseChaptersResponse

    @GET("get-categories2")
    suspend fun getCategories(
        @Header("Authorization") authHeader: String
    ): CategoriesResponse

    @GET("get-areas/{categoryId}")
    suspend fun getAreas(
        @Header("Authorization") token: String,
        @Path("categoryId") categoryId: Int
    ): AreasResponse

    @GET("area-all-courses/{areaId}")
    suspend fun getAreaCourses(
        @Header("Authorization") token: String,
        @Path("areaId") areaId: Int
    ): AreaAllCoursesResponse

    @GET("get-active-lessons2/{lessonID}")
    suspend fun getActiveLessons(
        @Header("Authorization") authHeader: String,
        @Path("lessonID") lessonID: Int
    ): ActiveLessonsResponse

    @GET("get-active-tasks2/{courseID}/{chapterID}/{lessonID}")
    suspend fun getActiveTasks(
        @Header("Authorization") authHeader: String,
        @Path("courseID") courseID: Int,
        @Path("chapterID") chapterID: Int,
        @Path("lessonID") lessonID: Int
    ): ActiveTasksResponse

    @POST("task-evaluate2")
    suspend fun evaluateTask(
        @Header("Authorization") authHeader: String,
        @Body request: JsonObject
    ): TaskEvaluationResponse

    @POST("write-user-course/{courseID}")
    suspend fun enrollInCourse(
        @Header("Authorization") authHeader: String,
        @Path("courseID") courseID: Int,
        @Body request: Map<String, String>
    ): retrofit2.Response<CourseEnrollmentResponse>

}

