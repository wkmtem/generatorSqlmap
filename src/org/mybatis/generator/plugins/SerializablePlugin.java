package org.mybatis.generator.plugins;

import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.mybatis.generator.api.CommentGenerator;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.InnerClass;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.TextElement;
  
/** 
 * MyBatis Generator生成DAO 的时候，生成的类都是没有序列化的
 * 用原来的源码添加了自己的代码，扩展一个 mybatis generator 插件，用于javaBean、Example对象实现序列化及序列化ID
 */  
public class SerializablePlugin extends PluginAdapter {  
  
    private FullyQualifiedJavaType serializable;  
    private FullyQualifiedJavaType gwtSerializable;  
    private boolean addGWTInterface;  
    private boolean suppressJavaInterface;  
  
    public SerializablePlugin() {  
        super();  
        serializable = new FullyQualifiedJavaType("java.io.Serializable"); //$NON-NLS-1$  
        gwtSerializable = new FullyQualifiedJavaType("com.google.gwt.user.client.rpc.IsSerializable"); //$NON-NLS-1$  
    }  
  
    public boolean validate(List<String> warnings) {  
        // this plugin is always valid  
        return true;  
    }  
  
    @Override  
    public void setProperties(Properties properties) {  
        super.setProperties(properties);  
        addGWTInterface = Boolean.valueOf(properties.getProperty("addGWTInterface")); //$NON-NLS-1$  
        suppressJavaInterface = Boolean.valueOf(properties.getProperty("suppressJavaInterface")); //$NON-NLS-1$  
    }  
  
    @Override  
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass,  
                                                 IntrospectedTable introspectedTable) {  
        makeSerializable(topLevelClass, introspectedTable);  
        return true;  
    }  
  
    @Override  
    public boolean modelPrimaryKeyClassGenerated(TopLevelClass topLevelClass,  
                                                 IntrospectedTable introspectedTable) {  
        makeSerializable(topLevelClass, introspectedTable);  
        return true;  
    }  
  
    @Override  
    public boolean modelRecordWithBLOBsClassGenerated(  
            TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {  
        makeSerializable(topLevelClass, introspectedTable);  
        return true;  
    }  
  
    /** 
     * 添加给Example类序列化的方法 
     * @param topLevelClass 
     * @param introspectedTable 
     * @return 
     */  
    @Override  
    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass,IntrospectedTable introspectedTable){  
        makeSerializable(topLevelClass, introspectedTable);  
  
        for (InnerClass innerClass : topLevelClass.getInnerClasses()) {  
            if ("GeneratedCriteria".equals(innerClass.getType().getShortName())) { //$NON-NLS-1$  
                innerClass.addSuperInterface(serializable);  
            }  
            if ("Criteria".equals(innerClass.getType().getShortName())) { //$NON-NLS-1$  
                innerClass.addSuperInterface(serializable);  
            }  
            if ("Criterion".equals(innerClass.getType().getShortName())) { //$NON-NLS-1$  
                innerClass.addSuperInterface(serializable);  
            }  
        }  
        // 分页
        addLimit(topLevelClass,introspectedTable,"limitStart");
        addLimit(topLevelClass,introspectedTable,"limitEnd");
        return super.modelExampleClassGenerated(topLevelClass,introspectedTable);
        //return true;  
    }  
  
    protected void makeSerializable(TopLevelClass topLevelClass,  
                                    IntrospectedTable introspectedTable) {  
        if (addGWTInterface) {  
            topLevelClass.addImportedType(gwtSerializable);  
            topLevelClass.addSuperInterface(gwtSerializable);  
        }  
  
        if (!suppressJavaInterface) {  
            topLevelClass.addImportedType(serializable);  
            topLevelClass.addSuperInterface(serializable);  
  
            Field field = new Field();  
            field.setFinal(true);
            Random random = new Random(); 
            field.setInitializationString(random.nextLong()+"L"); //$NON-NLS-1$  
            field.setName("serialVersionUID"); //$NON-NLS-1$  
            field.setStatic(true);  
            field.setType(new FullyQualifiedJavaType("long")); //$NON-NLS-1$  
            field.setVisibility(JavaVisibility.PRIVATE);  
            context.getCommentGenerator().addFieldComment(field, introspectedTable);  
  
            topLevelClass.addField(field);  
        }  
    } 
    
    
    /**
     * 
     * <if test="">标签
     */
    @Override
    public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(XmlElement element, 
    		IntrospectedTable introspectedTable) {

           // LIMIT ${limitStart}, ${limitEnd} LIMIT 5,10; // 检索记录行 6-15
           XmlElement isNotNullElement = new XmlElement("if");//$NON-NLS-1$
           isNotNullElement.addAttribute(new Attribute("test", "limitStart != null and limitStart >= 0"));//$NON-NLS-1$ //$NON-NLS-2$
           isNotNullElement.addElement(new TextElement("LIMIT ${limitStart}, ${limitEnd}"));
           element.addElement(isNotNullElement);

           // ${limitEnd} LIMIT 5;//检索前 5个记录行
           return super.sqlMapUpdateByExampleWithoutBLOBsElementGenerated(element, introspectedTable);
    }
    
    /**
     * example add方法
     */
    private void addLimit(TopLevelClass topLevelClass, IntrospectedTable introspectedTable, String name){

        CommentGenerator commentGenerator = context.getCommentGenerator();

        Field field = new Field();

        field.setVisibility(JavaVisibility.PROTECTED);
        field.setType(FullyQualifiedJavaType.getIntInstance());
        field.setName(name);
        field.setInitializationString("-1");

        commentGenerator.addFieldComment(field, introspectedTable);

        topLevelClass.addField(field);

        char c = name.charAt(0);

        String camel = Character.toUpperCase(c) + name.substring(1);

        Method method = new Method();
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setName("set" + camel);
        method.addParameter(new Parameter(FullyQualifiedJavaType.getIntInstance(), name));
        method.addBodyLine("this." + name + "=" + name + ";");

        commentGenerator.addGeneralMethodComment(method, introspectedTable);

        topLevelClass.addMethod(method);

        method = new Method();
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(FullyQualifiedJavaType.getIntInstance());
        method.setName("get" + camel);
        method.addBodyLine("return " + name + ";");

        commentGenerator.addGeneralMethodComment(method, introspectedTable);

        topLevelClass.addMethod(method);
    }
    
}  