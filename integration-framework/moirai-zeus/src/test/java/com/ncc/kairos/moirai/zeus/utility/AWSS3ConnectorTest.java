package com.ncc.kairos.moirai.zeus.utility;

import com.amazonaws.services.s3.AmazonS3;
import com.ncc.kairos.moirai.zeus.utililty.AWSS3Connector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

@TestPropertySource(locations = "classpath:test.properties")
public class AWSS3ConnectorTest {

    // This needs to change, but am having issues with @Mock
    @Autowired
    AWSS3Connector connector;

    @Mock
    AmazonS3 s3;

    private void updateReflections() {
        ReflectionTestUtils.setField(connector, "s3", s3);
    }

    @BeforeEach
    public void setup() {
        updateReflections();
    }

    @Test
    public void getRegistryFromKeyTest() {
        String validMatch = "something/repo/test/docker/v3esdfw";
        String result = connector.getRegistryFromKey(validMatch);
        assert (result.equals("test"));

        String invalidMatch = "weewewef/awdaw/gregs/awf/hh/h/t/dr/r";
        result = connector.getRegistryFromKey(invalidMatch);
        assert (result.equals(""));
    }

    @Test
    public void getRepoFromKeyTest() {
        String validMatch = "/weewewe/repositories/thiswillreturn/_manifests/A32r2j23r92u23r982h";
        String result = connector.getRepoFromKey(validMatch);
        assert (result.equals("thiswillreturn"));

        String invalidMatch = "weewewef/repositories/gregs/awf/hh/h/t/dr/r";
        result = connector.getRegistryFromKey(invalidMatch);
        assert (result.equals(""));
    }

    @Test
    public void getTagFromKeyTest() {
        String validMatch = "/weewewe/tags/thiswillreturn/current/A32r2j23r92u23r982h";
        String result = connector.getTagFromKey(validMatch);
        assert (result.equals("thiswillreturn"));

        String invalidMatch = "weewewef/repositories/gregs/current/hh/h/t/dr/r";
        result = connector.getTagFromKey(invalidMatch);
        assert (result.equals(""));
    }
    
    @Test
    void getDockerRegistryS3DataTest() {

        // static mock
        // s3.listObjects(bucketName, prefix);

        // test same repos with different tags and same digest; exp -> combined data
        // test different repos with same tags and different digest; exp -> different data
        // test different repos with different tags and same digests; exp -> different data
    }
}
