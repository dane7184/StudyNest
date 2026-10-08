/**
 * Precompiled [lms.spring-service.gradle.kts][Lms_spring_service_gradle] script plugin.
 *
 * @see Lms_spring_service_gradle
 */
public
class Lms_springServicePlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Lms_spring_service_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
