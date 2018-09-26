package de.tub.mcc.fogmock.nodemanager.graphserv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class InfrastructureController {
    private static InfrastructureController instance;
    ArrayList<String> currentLog = new ArrayList<>();
    String currentPlatform = null;
    String currentPlaybook = null;
    private static Logger logger = LoggerFactory.getLogger(InfrastructureController.class);

    public static InfrastructureController getInstance () throws IOException {
        if (InfrastructureController.instance == null) {
            InfrastructureController.instance = new InfrastructureController();
        }
        return InfrastructureController.instance;
    }

    InfrastructureController() throws IOException {
        // cloneMockFogIaCRepository("git clone git@github.com:OpenFogStack/MockFog-IaC.git --branch master --single-branch");
        // cloneMockFogIaCRepository("git pull git@github.com:OpenFogStack/MockFog-IaC.git --branch master --single-branch");
        // runCommand("git checkout develop", false);
        // setOpenStackSSHKey("elias", "MockFog-IaC/os_example_vars.yml");
        // setCurrentDirectory("MockFog-IaC/openstack.yml");
        // runCommand("virtualenv MockFog-IaC/.venv", true);
        // runCommand("bash -c 'source MockFog-IaC/.venv/bin/activate'", true);
        // runCommand("bash -c 'source MockFog-openrc.sh'", false);
        // runCommand("pip install -r MockFog-IaC/requirements-dev.txt", true);

        //String[] bootstrapAnsible = { "ansible-playbook MockFog-IaC/openstack.yml --tags \"bootstrap\" -vvv" };
        //runCommands(bootstrapAnsible, true);

        //String[] gitStashIaC = { "bash -c 'cd MockFog-IaC/'", "git stash" };
        //runCommands(gitStashIaC, true);
    }


    /** This method initiates the bootstrapping process by
     *  checking the platform (true for OpenStack, false for AWS)and
     *  selecting the bootstrap playbook and
     *  starting the playbook.
     *
     * @param platformIsOpenStack the param for checking the IaaS Provider
     * @return
     */
    public String bootstrapSetup (boolean platformIsOpenStack) {
        currentPlaybook = "bootstrap";
        currentPlatform = platformIsOpenStack ? "openstack.yml" : "aws.yml";
        logger.info("Start bootstrapping with Ansible, platform is OpenStack=" + platformIsOpenStack);
        return startAnsiblePlaybook("[bootstrap] Ansible is bootstraping the infrastructure. Please wait until the network topology refreshes.");
    }

    /** This method destroys the bootstrapped environment by
     *  checking the platform (true for OpenStack, false for AWS)and
     *  selecting the destroy playbook and
     *  starting the playbook.
     *
     * @param platformIsOpenStack the param for checking the IaaS Provider
     * @return
     */
    public String destroySetup(boolean platformIsOpenStack) {
        currentPlaybook = "destroy";
        currentPlatform = platformIsOpenStack ? "openstack.yml" : "aws.yml";
        return startAnsiblePlaybook("[destroy] Ansible is destroying the infrastructure. Please wait until the machines and networks are deleted.");
    }

    /** This method starts an ansible playbook (either bootstrap or destroy) by running the respective command.
     *
     * @param successfulReturnMessage the message informing about the process status
     * @return
     */
    public String startAnsiblePlaybook(String successfulReturnMessage) {
        try {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        String[] commands = {"ansible-playbook", ("/opt/MFog-IaC/" + currentPlatform), "--tags", currentPlaybook };
                        runCommands(commands, true); }
                    catch (Exception e) {
                        logger.error("Error with ansible inside it's Thread!: ", e);
                    }
                }
            });
            thread.start();
        }
        catch (Exception e) {
            logger.error("Error with ansible!: ", e);
            return "";
        }
        return successfulReturnMessage;
    }


    public void destroySetup() {
        runCommand("ansible-playbook /opt/MFog-IaC/openstack.yml --tags \"destroy\" -vvv ", true);
    }

    private void setOpenStackSSHKey(String sshKey, String filePath) throws IOException {
        List<String> fileContent = new ArrayList<>(Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8));
        fileContent.set(3, "os_ssh_key_name: " + sshKey);
        fileContent.remove(2);
        Files.write(Paths.get(filePath), fileContent, StandardCharsets.UTF_8);
    }

    private void setCurrentDirectory(String filePath) throws IOException {
        List<String> fileContent = new ArrayList<>(Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8));
        String dir = "System.getProperty(\"user.dir\")";
        fileContent.set(3, "    - " + dir + "/os_example_vars.yml");
        fileContent.remove(4);
        Files.write(Paths.get(filePath), fileContent, StandardCharsets.UTF_8);
    }



    private void cloneMockFogIaCRepository(String command) {
        Process process = runCommand(command, false);

        Reader r = new InputStreamReader(process.getInputStream());
        BufferedReader in = new BufferedReader(r);
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("ERROR")) {

                    System.out.println("Error runnning command: " + command + "!");
                    break;
                }
            }
            in.close();
        } catch (IOException e) {
            logger.error("IOException while cloning respository: ", e);
        }
    }


    /** This method runs a command in the console.
     *
     * @param command the command to be executed
     * @param printOutputToConsole the param to check if the output should be displayed in the console
     * @return
     */
    private Process runCommand(String command, boolean printOutputToConsole) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            logger.error("IOException while running " + command, e);
        }
        if (printOutputToConsole)
            printOutputToConsole(process);
        return process;
    }

    /** This method runs multiple commands in the console.
     *
     * @param commands the list of the commands to be executed
     * @param printOutputToConsole the param to check if the output should be displayed in the console
     * @return
     */
    private Process runCommands(String[] commands, boolean printOutputToConsole) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(commands);
        } catch (IOException e) {
            logger.error("IOException while running commands (" + commands.length + "): ", e);
        }
        if (printOutputToConsole)
            printOutputToConsole(process);
        return process;
    }

    /** This method prints the output of the given process in the console.
     *
     * @param process the process which output should be displayed
     */
    private void printOutputToConsole(Process process) {
        // inspired by https://stackoverflow.com/questions/5711084/java-runtime-getruntime-getting-output-from-executing-a-command-line-program
        try {
            currentLog.clear();

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            // read the output from the command
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
                currentLog.add("[?] " + s);
            }

            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
                currentLog.add("[!] " + s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
