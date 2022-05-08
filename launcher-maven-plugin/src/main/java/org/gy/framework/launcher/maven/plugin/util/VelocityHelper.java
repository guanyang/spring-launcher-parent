package org.gy.framework.launcher.maven.plugin.util;

import java.io.StringWriter;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;

public class VelocityHelper {

    private static final String TEMPLATE_NAME = "TEMPLATE_NAME";

    private VelocityHelper(){

    }

    public static String render(String templateStr, Map<String,String> params){
        VelocityEngine ve = initVelocityEngine();

        StringResourceRepository repo = (StringResourceRepository) ve.getApplicationAttribute(StringResourceLoader.REPOSITORY_NAME_DEFAULT);
        repo.putStringResource(TEMPLATE_NAME, templateStr);

        // Set parameters for my template.
        VelocityContext context = new VelocityContext(params);

        // Get and merge the template with my parameters.
        Template template = ve.getTemplate(TEMPLATE_NAME);
        StringWriter writer = new StringWriter();
        template.merge(context, writer);

        return writer.toString();
    }

    private static VelocityEngine initVelocityEngine(){
        VelocityEngine engine = new VelocityEngine();
        engine.setProperty(Velocity.RESOURCE_LOADER, "string");
        engine.addProperty("string.resource.loader.class", StringResourceLoader.class.getName());
        engine.addProperty("string.resource.loader.repository.static", "false");
        engine.init();
        return engine;
    }

}
