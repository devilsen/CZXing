package me.devilsen.czxing.compat;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * desc :
 * date : 12/7/20 5:51 PM
 *
 * @author : dongSen
 */
@Retention(CLASS)
@Target({TYPE, METHOD, CONSTRUCTOR, FIELD, PACKAGE})
public @interface BarRequiresApi {
    /**
     * The API level to require. Alias for {@link #api} which allows you to leave out the {@code
     * api=} part.
     */
    int value() default 1;

    /**
     * The API level to require
     */
    int api() default 1;
}