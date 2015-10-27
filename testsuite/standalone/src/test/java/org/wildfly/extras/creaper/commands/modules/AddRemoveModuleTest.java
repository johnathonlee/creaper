package org.wildfly.extras.creaper.commands.modules;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import org.wildfly.extras.creaper.test.WildFlyTests;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.wildfly.extras.creaper.XmlAssert.assertXmlIdentical;

@RunWith(Arquillian.class)
@Category(WildFlyTests.class)
public class AddRemoveModuleTest {
    private static final String TEST_MODULE_NAME = "org.jboss.testmodule";
    private static final String EXPECTED_MODULE_XML = ""
            + "<?xml version=\"1.0\" ?>\n"
            + "\n"
            + "<module xmlns=\"urn:jboss:module:1.1\" name=\"" + TEST_MODULE_NAME + "\" slot=\"main\">\n"
            + "\n"
            + "    <properties>\n"
            + "        <property name=\"foo\" value=\"bar\"/>\n"
            + "        <property name=\"john\" value=\"doe\"/>\n"
            + "    </properties>\n"
            + "\n"
            + "    <main-class value=\"test.mainclass\"/>\n"
            + "\n"
            + "    <resources>\n"
            + "        <resource-root path=\"testJar1.jar\"/>\n"
            + "        <resource-root path=\"testJar2.jar\"/>\n"
            + "    </resources>\n"
            + "\n"
            + "    <dependencies>\n"
            + "        <module name=\"org.hibernate\"/>\n"
            + "        <module name=\"org.jboss.as.controller\"/>\n"
            + "    </dependencies>\n"
            + "</module>";

    private OnlineManagementClient client;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Before
    public void connect() throws IOException {
        client = ManagementClient.online(OnlineOptions.standalone().localDefault().build());
    }

    @After
    public void close() throws IOException {
        client.close();
    }

    @Test
    public void addRemoveModuleTest() throws IOException, CommandFailedException, CliException, SAXException {
        // create test module
        File testJar1 = createTestJar("testJar1.jar", "folder with spaces");
        File testJar2 = createTestJar("testJar2.jar", null);
        AddModule addModule = new AddModule.Builder(TEST_MODULE_NAME)
                .resource(testJar1)
                .resource(testJar2)
                .resourceDelimiter(":")
                .dependency("org.hibernate")
                .dependency("org.jboss.as.controller")
                .mainClass("test.mainclass")
                .property("foo", "bar")
                .property("john", "doe")
                .build();
        client.apply(addModule);

        // verify that module was added
        File asRoot = new File(getPathToAs());

        File module = new File(asRoot, "modules" + File.separator + TEST_MODULE_NAME.replaceAll("\\.", File.separator));
        assertTrue("Module " + module.getAbsolutePath() + " should exist on path", module.exists());

        File moduleTestJar1 = new File(module, "main" + File.separator + testJar1.getName());
        assertTrue("File " + moduleTestJar1.getAbsolutePath() + " should exist", moduleTestJar1.exists());
        File moduleTestJar2 = new File(module, "main" + File.separator + testJar2.getName());
        assertTrue("File " + moduleTestJar2.getAbsolutePath() + " should exist", moduleTestJar2.exists());

        File moduleXml = new File(module, "main" + File.separator + "module.xml");
        assertTrue("File " + moduleXml.getName() + " should exist in " + module.getAbsolutePath(), moduleXml.exists());

        assertXmlIdentical(EXPECTED_MODULE_XML, Files.toString(moduleXml, Charsets.UTF_8));

        // remove test module
        RemoveModule removeModule = new RemoveModule(TEST_MODULE_NAME);
        client.apply(removeModule);

        // verify that module was removed
        assertFalse("Module shouldn't exist on path " + module.getAbsolutePath(), module.exists());
    }

    private File createTestJar(String fileName, String folder) throws IOException {
        File testJar = null;
        if (folder != null) {
            File folderWithSpacesInName = tmp.newFolder(folder);
            testJar = new File(folderWithSpacesInName, fileName);
        } else {
            testJar = tmp.newFile(fileName);
        }
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .addClass(AddRemoveModuleTest.class);
        jar.as(ZipExporter.class).exportTo(testJar, true);
        return testJar;
    }

    private String getPathToAs() throws IOException, CliException {
        ModelNodeResult result = client.execute(":resolve-expression(expression=${jboss.home.dir})");
        result.assertSuccess("Cannot resolve jboss.home.dir");
        return result.stringValue();
    }
}
