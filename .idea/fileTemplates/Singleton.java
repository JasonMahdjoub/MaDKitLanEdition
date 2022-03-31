#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end
#parse("File Header.java")
#set ($AUTHOR_NAME="Jason Mahdjoub")
/**
 * @author ${AUTHORNAME}
 * @version 1.0
 * @since ${PROJECT_NAME} ${SINCE_PROJECT_VERSION} 
 */
public class ${NAME}{
    private static ${NAME} ourInstance = new ${NAME}();

    public static ${NAME} getInstance() {
        return ourInstance;
    }

    private ${NAME}() {
    }
}
