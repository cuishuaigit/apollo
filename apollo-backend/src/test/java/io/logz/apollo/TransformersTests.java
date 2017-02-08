package io.logz.apollo;

import io.logz.apollo.auth.User;
import io.logz.apollo.dao.DeployableVersionDao;
import io.logz.apollo.dao.DeploymentDao;
import io.logz.apollo.dao.EnvironmentDao;
import io.logz.apollo.dao.ServiceDao;
import io.logz.apollo.dao.UserDao;
import io.logz.apollo.database.ApolloMyBatis;
import io.logz.apollo.excpetions.ApolloParseException;
import io.logz.apollo.helpers.ModelsGenerator;
import io.logz.apollo.helpers.StandaloneApollo;
import io.logz.apollo.kubernetes.ApolloToKubernetes;
import io.logz.apollo.models.DeployableVersion;
import io.logz.apollo.models.Deployment;
import io.logz.apollo.models.Environment;
import io.logz.apollo.models.Service;
import org.junit.Test;

import javax.script.ScriptException;
import java.io.IOException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by roiravhon on 2/1/17.
 */
public class TransformersTests {

    private final String DEFAULT_LABEL_KEY = "app";
    private final String DEFAULT_LABEL_VALUE = "nginx";
    private final EnvironmentDao environmentDao;
    private final ServiceDao serviceDao;
    private final DeployableVersionDao deployableVersionDao;
    private final UserDao userDao;
    private final DeploymentDao deploymentDao;

    private class CreateDeploymentResult {
        private final Deployment deployment;
        private final DeployableVersion deployableVersion;
        private final Environment environment;

        CreateDeploymentResult(Deployment deployment, DeployableVersion deployableVersion, Environment environment) {
            this.deployment = deployment;
            this.deployableVersion = deployableVersion;
            this.environment = environment;
        }

        Deployment getDeployment() {
            return deployment;
        }

        DeployableVersion getDeployableVersion() {
            return deployableVersion;
        }

        public Environment getEnvironment() {
            return environment;
        }
    }

    public TransformersTests() throws ScriptException, IOException, SQLException {

        // We just need to make sure we have a DB instance running, since this class does not uses apollo client
        StandaloneApollo.getOrCreateServer();

        // Get the DAOs we need
        environmentDao = ApolloMyBatis.getDao(EnvironmentDao.class);
        serviceDao = ApolloMyBatis.getDao(ServiceDao.class);
        deployableVersionDao = ApolloMyBatis.getDao(DeployableVersionDao.class);
        userDao = ApolloMyBatis.getDao(UserDao.class);
        deploymentDao = ApolloMyBatis.getDao(DeploymentDao.class);
    }

    @Test
    public void testImageNameTransformer() throws ApolloParseException {
        String imageNameWithRepoAndVersion = "repo:1234/image:version";
        String imageNameWithRepoAndNoVersion = "repo:1234/image";
        String imageNameWithSimpleRepoAndNoVersion = "repo/image";
        String imageNameWithNoRepoAndVersion = "image:version";
        String imageNameWithNoRepoAndNoVersion = "image";

        CreateDeploymentResult createDeploymentResult;
        ApolloToKubernetes apolloToKubernetes;

        createDeploymentResult = createDeployment(imageNameWithRepoAndVersion, "key", "value");
        apolloToKubernetes = new ApolloToKubernetes(createDeploymentResult.getDeployment());
        assertImageName(apolloToKubernetes.getKubernetesDeployment(), imageNameWithRepoAndVersion);

        createDeploymentResult = createDeployment(imageNameWithRepoAndNoVersion, "key", "value");
        apolloToKubernetes = new ApolloToKubernetes(createDeploymentResult.getDeployment());
        assertImageName(apolloToKubernetes.getKubernetesDeployment(), imageNameWithRepoAndNoVersion + ":" + createDeploymentResult.getDeployableVersion().getGitCommitSha());

        createDeploymentResult = createDeployment(imageNameWithSimpleRepoAndNoVersion, "key", "value");
        apolloToKubernetes = new ApolloToKubernetes(createDeploymentResult.getDeployment());
        assertImageName(apolloToKubernetes.getKubernetesDeployment(), imageNameWithSimpleRepoAndNoVersion + ":" + createDeploymentResult.getDeployableVersion().getGitCommitSha());

        createDeploymentResult = createDeployment(imageNameWithNoRepoAndVersion, "key", "value");
        apolloToKubernetes = new ApolloToKubernetes(createDeploymentResult.getDeployment());
        assertImageName(apolloToKubernetes.getKubernetesDeployment(), imageNameWithNoRepoAndVersion);

        createDeploymentResult = createDeployment(imageNameWithNoRepoAndNoVersion, "key", "value");
        apolloToKubernetes = new ApolloToKubernetes(createDeploymentResult.getDeployment());
        assertImageName(apolloToKubernetes.getKubernetesDeployment(), imageNameWithNoRepoAndNoVersion + ":" + createDeploymentResult.getDeployableVersion().getGitCommitSha());
    }

    @Test
    public void testDeploymentLabelsTransformer() throws ApolloParseException {

        CreateDeploymentResult createDeploymentResult;
        ApolloToKubernetes apolloToKubernetes;

        String SampleLabelFromTransformer = "environment";

        createDeploymentResult = createDeployment("image", "key", "value");
        apolloToKubernetes = new ApolloToKubernetes(createDeploymentResult.getDeployment());
        assertDeploymentLabelExists(apolloToKubernetes.getKubernetesDeployment(), DEFAULT_LABEL_KEY, DEFAULT_LABEL_VALUE);

        // Check for one of the default labels that the transformer assigns
        createDeploymentResult = createDeployment("image", "key", "value");
        apolloToKubernetes = new ApolloToKubernetes(createDeploymentResult.getDeployment());
        assertDeploymentLabelExists(apolloToKubernetes.getKubernetesDeployment(),
                SampleLabelFromTransformer, createDeploymentResult.getEnvironment().getName());

        // Check that the transformer does not override a given label with a default one
        createDeploymentResult = createDeployment("image", SampleLabelFromTransformer, "value");
        apolloToKubernetes = new ApolloToKubernetes(createDeploymentResult.getDeployment());
        assertDeploymentLabelExists(apolloToKubernetes.getKubernetesDeployment(), SampleLabelFromTransformer, "value");
    }

    @Test
    public void testServiceLabelsTransformer() throws ApolloParseException {

        CreateDeploymentResult createDeploymentResult;
        ApolloToKubernetes apolloToKubernetes;

        String SampleLabelFromTransformer = "environment";

        createDeploymentResult = createDeployment("image", "key", "value");
        apolloToKubernetes = new ApolloToKubernetes(createDeploymentResult.getDeployment());
        assertServiceLabelExists(apolloToKubernetes.getKubernetesService(), DEFAULT_LABEL_KEY, DEFAULT_LABEL_VALUE);

        createDeploymentResult = createDeployment("image", "key", "value");
        apolloToKubernetes = new ApolloToKubernetes(createDeploymentResult.getDeployment());
        assertServiceLabelExists(apolloToKubernetes.getKubernetesService(),
                SampleLabelFromTransformer, createDeploymentResult.getEnvironment().getName());
    }

    private void assertImageName(io.fabric8.kubernetes.api.model.extensions.Deployment deployment, String imageName) {
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().stream().findFirst().get().getImage()).isEqualTo(imageName);
    }

    private void assertDeploymentLabelExists(io.fabric8.kubernetes.api.model.extensions.Deployment deployment, String labelKey, String labelValue) {
        assertThat(deployment.getSpec().getTemplate().getMetadata().getLabels().get(labelKey)).isEqualTo(labelValue);
    }

    private void assertServiceLabelExists(io.fabric8.kubernetes.api.model.Service service, String labelKey, String labelValue) {
        assertThat(service.getMetadata().getLabels().get(labelKey)).isEqualTo(labelValue);
    }

    private CreateDeploymentResult createDeployment(String deploymentImageName, String extraLabelKey, String extraLabelValue) {

        // Create all models in DB
        Environment testEnvironment = ModelsGenerator.createEnvironment();
        environmentDao.addEnvironment(testEnvironment);

        Service testService = ModelsGenerator.createService();
        testService.setDeploymentYaml(getDeploymentKubernetesYaml(deploymentImageName, extraLabelKey, extraLabelValue));
        testService.setServiceYaml(getServiceDeploymentYaml(extraLabelKey, extraLabelValue));
        serviceDao.addService(testService);

        DeployableVersion testDeployableVersion = ModelsGenerator.createDeployableVersion(testService);
        deployableVersionDao.addDeployableVersion(testDeployableVersion);

        User testUser = ModelsGenerator.createRegularUser();
        userDao.addUser(testUser);

        Deployment testDeployment = ModelsGenerator.createDeployment(testService, testEnvironment, testDeployableVersion, testUser);
        testDeployment.setStatus(Deployment.DeploymentStatus.PENDING);
        deploymentDao.addDeployment(testDeployment);

        return new CreateDeploymentResult(testDeployment, testDeployableVersion, testEnvironment);
    }

    private String getDeploymentKubernetesYaml(String imageName, String extraLabelKey, String extraLabelValue) {

        return "apiVersion: extensions/v1beta1\n" +
                "kind: Deployment\n" +
                "metadata:\n" +
                "  labels:\n" +
                "    tahat: nginx\n" +
                "  name: nginx\n" +
                "  namespace: default\n" +
                "spec:\n" +
                "  replicas: 1\n" +
                "  strategy:\n" +
                "    rollingUpdate:\n" +
                "      maxSurge: 1\n" +
                "      maxUnavailable: 0\n" +
                "    type: RollingUpdate\n" +
                "  template:\n" +
                "    metadata:\n" +
                "      labels:\n" +
                "        " + DEFAULT_LABEL_KEY + ": " + DEFAULT_LABEL_VALUE + "\n" +
                "        " + extraLabelKey + ": " + extraLabelValue + "\n" +
                "    spec:\n" +
                "      containers:\n" +
                "      - image: " + imageName + "\n" +
                "        imagePullPolicy: Always\n" +
                "        name: roi-apollo-test\n" +
                "        ports:\n" +
                "        - containerPort: 80\n" +
                "          protocol: TCP\n" +
                "        resources: {}\n" +
                "      dnsPolicy: ClusterFirst\n" +
                "      restartPolicy: Always\n" +
                "      securityContext: {}\n" +
                "      terminationGracePeriodSeconds: 30";
    }

    private String getServiceDeploymentYaml(String extraLabelKey, String extraLabelValue) {
        return "apiVersion: v1\n" +
                "kind: Service\n" +
                "metadata:\n" +
                "  labels:\n" +
                "    " + DEFAULT_LABEL_KEY + ": " + DEFAULT_LABEL_VALUE + "\n" +
                "    " + extraLabelKey + ": " + extraLabelValue + "\n" +
                "  name: roi-test-service\n" +
                "  namespace: default\n" +
                "spec:  \n" +
                "  ports:\n" +
                "  - nodePort: 30002\n" +
                "    port: 80\n" +
                "    protocol: TCP\n" +
                "    targetPort: 80\n" +
                "  selector:\n" +
                "    app: nginx\n" +
                "  sessionAffinity: None\n" +
                "  type: NodePort\n" +
                "status:\n" +
                "  loadBalancer: {}";
    }
}