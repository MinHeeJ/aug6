package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Ensures Spring can select the mapper constructor when creating the validator bean. */
class EducationAchievementAccessValidatorWiringTest {
    @Test
    void mapperConstructorIsExplicitlyAutowiredForSpringBeanCreation() {
        Constructor<?> mapperConstructor = findMapperConstructor();

        assertThat(mapperConstructor.isAnnotationPresent(Autowired.class)).isTrue();
    }

    private Constructor<?> findMapperConstructor() {
        for (Constructor<?> constructor : EducationAchievementAccessValidator.class.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 1
                    && constructor.getParameterTypes()[0] == EducationAchievementAccessMapper.class) {
                return constructor;
            }
        }
        throw new AssertionError("EducationAchievementAccessValidator mapper constructor is missing");
    }
}
