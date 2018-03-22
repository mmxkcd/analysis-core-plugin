package io.jenkins.plugins.analysis.core.steps;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.analysis.core.steps.IssuesRecorder.Descriptor;
import static io.jenkins.plugins.analysis.core.testutil.FormValidationAssert.assertThat;

import hudson.util.FormValidation;

class IssuesRecorderTest {

    @Test
    void shouldBeOkIfEncodingIsEmpty(){
        //given
        Descriptor descriptor = new Descriptor();
        //when
        FormValidation actualResult = descriptor.doCheckSourceCodeEncoding("");
        //then
        assertThat(actualResult).isOk();
    }
}