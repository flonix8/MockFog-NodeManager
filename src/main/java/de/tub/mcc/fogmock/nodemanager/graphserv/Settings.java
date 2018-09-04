package de.tub.mcc.fogmock.nodemanager.graphserv;

public class Settings {

    //TODO: before pushing
    /*
    For the live testcase on the Nodemanager running in Docker (mounted directories)
     */
    public final static String PATH_TO_OS_VARS =  "/opt/MFog-IaC/os_vars.yml";
    public final static String PATH_TO_AWS_VARS =  "/opt/MFog-IaC/aws_vars.yml";

    public final static String PATH_TO_OS_CONFIG =  "/opt/MFog-IaC/os_config.yml";
    public final static String PATH_TO_AWS_CONFIG =  "/opt/MFog-IaC/aws_config.yml";

    public final static String PATH_TO_OS_FLAVORS = "/opt/MFog-files/os_device_to_flavor_map.json";
    public final static String PATH_TO_AWS_FLAVORS = "/opt/MFog-files/aws_device_to_flavor_map.json";

    /*
     For the local testcase without Docker
     */
//    public final static String PATH_TO_OS_VARS =  "MockFog-IaC/os_vars.yml";
//    public final static String PATH_TO_AWS_VARS =  "MockFog-IaC/aws_vars.yml";
//
//    public final static String PATH_TO_OS_CONFIG =  "MockFog-IaC/os_config.yml";
//    public final static String PATH_TO_AWS_CONFIG =  "MockFog-IaC/aws_config.yml";
//
//    public final static String PATH_TO_OS_FLAVORS =  "files/os_device_to_flavor_map.json";
//    public final static String PATH_TO_AWS_FLAVORS =  "files/aws_device_to_flavor_map.json";

}
