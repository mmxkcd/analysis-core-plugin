package io.jenkins.plugins.analysis.core.model;

import hudson.model.Result;
import hudson.model.Run;
import io.jenkins.plugins.analysis.core.JenkinsFacade;
import io.jenkins.plugins.analysis.core.model.Summary.LabelProviderFactoryFacade;
import io.jenkins.plugins.analysis.core.quality.AnalysisBuild;
import io.jenkins.plugins.analysis.core.quality.QualityGate;
import io.jenkins.plugins.analysis.core.quality.Thresholds;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link Summary}.
 *
 * @author Ullrich Hafner
 * @author Michaela Reitschuster
 */
class SummaryTest {
    /**
     * Tests if errors in the AnalysisResult result in an exclamation triangle icon in the created HTML.
     */
    @Test
    void shouldHaveDivWithTriangleIcon() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 0, 0, Lists.immutable.of("Error 1", "Error 2"), 0, 0);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).contains("class=\"fa fa-exclamation-triangle\"");
    }

    /**
     * Tests if no errors in the AnalysisResult result in an info icon in the created HTML.
     */
    @Test
    void shouldHaveDivWithInfoIcon() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 0, 0, Lists.immutable.of(), 0, 0);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).contains("<a href=\"testResult/info\"><i class=\"fa fa-info-circle\"></i>");
    }

    /**
     * Tests if the correct ids are included in the created HTML.
     */
    @Test
    void shouldHaveCorrectIDsForDivs() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 0, 0, Lists.immutable.of(), 0, 0);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).contains("<div id=\"test-summary\">");
        assertThat(createdHtml).contains("<div id=\"test-title\">");
    }

    /**
     * Tests if there is no message for toolnames in the created HTML when the AnalysisResult contains an empty sizePerOrigin.
     */
    @Test
    void shouldContainNoMessageForToolNames() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 0, 0, Lists.immutable.empty(), 0, 0);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).doesNotContain(Messages.Tool_ParticipatingTools(""));
    }

    /**
     * Tests if the created HTML contains the toolnames from the AnalysisResult.
     */
    @Test
    void shouldContainMessageWithToolNames() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of("checkstyle", 15, "pmd", 20), 0, 0, Lists.immutable.empty(), 0, 0);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).contains(Messages.Tool_ParticipatingTools("CheckStyle, PMD"));
    }

    /**
     * Tests if the createdHtml shows a message that no new issues have occurred for a number of builds.
     */
    @Test
    void shouldContainNoIssuesSinceLabel() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 0, 0, Lists.immutable.empty(), 0, 0);
        when(analysisResult.getTotalSize()).thenReturn(0);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).containsPattern("No warnings for .* builds");
    }

    /**
     * Tests if the created HTML does not contain the label for no issues since when issues have occurred.
     */
    @Test
    void shouldNotContainNoIssuesSinceLabelWhenTotalIsNotZero() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 0, 0, Lists.immutable.empty(), 0, 0);
        when(analysisResult.getTotalSize()).thenReturn(1);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).doesNotContain("No warnings for");
        assertThat(createdHtml).contains("<a href=\"testResult\">One warning</a>");
    }

    /**
     * Tests if the created HTML does not contain the label for no issues since when the current build is younger.
     */
    @Test
    void shouldNotContainNoIssuesSinceLabelWhenBuildIsYounger() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 0, 0, Lists.immutable.empty(), 0, 0);
        when(analysisResult.getTotalSize()).thenReturn(0);
        AnalysisBuild build = mock(AnalysisBuild.class);
        when(build.getNumber()).thenReturn(1);
        when(analysisResult.getBuild()).thenReturn(build);
        when(analysisResult.getNoIssuesSinceBuild()).thenReturn(3);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).doesNotContain("No warnings for");
        assertThat(createdHtml).contains("No warnings");
    }

    /**
     * Tests if the created HTML shows a message for new warnings that occured in the AnalysisResult.
     */
    @Test
    void shouldContainNewIssues() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 3, 0, Lists.immutable.empty(), 0, 0);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).containsPattern(
                createWarningsLink("<a href=\"testResult/new\">.*3 new warnings.*</a>"));
    }

    /**
     * Tests if the created HTML does not include a message for new warnings when none are present in the AnalysisResult.
     */
    @Test
    void shouldNotContainNewIssues() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 0, 0, Lists.immutable.empty(), 0, 0);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).doesNotContainPattern(
                createWarningsLink("<a href=\"testResult/new\">.* new warnings.*</a>"));
    }

    /**
     * Tests if a message for fixed issues is included in the HTML.
     */
    @Test
    void shouldContainFixedIssuesLabel() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 0, 5, Lists.immutable.empty(), 0, 0);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).containsPattern(
                createWarningsLink("<a href=\"testResult/fixed\">.*5 fixed warnings.*</a>"));
    }

    /**
     * Tests if no message for fixed issues is included in the HTML when none have been fixed.
     */
    @Test
    void shouldNotContainFixedIssuesLabel() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 0, 0, Lists.immutable.empty(), 0, 0);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).doesNotContainPattern(
                createWarningsLink("<a href=\"testResult/fixed\">.* fixed warnings.*</a>"));
    }

    /**
     * Tests if the QualityGateResult is included in the HTML when it's enabled.
     */
    @Test
    void shouldContainQualityGateResult() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 0, 0, Lists.immutable.empty(), 0, 0);
        when(analysisResult.getOverallResult()).thenReturn(Result.SUCCESS);
        QualityGate qualityGate = mock(QualityGate.class);
        when(qualityGate.isEnabled()).thenReturn(true);
        when(analysisResult.getQualityGate()).thenReturn(qualityGate);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).containsPattern(
                createWarningsLink("Quality gate: <img src=\"color\" class=\"icon-blue icon-lg\" alt=\"Success\" title=\"Success\"> Success"));
    }

    /**
     * Tests if the QualityGateResult is not included in the HTML when it is not enabled in the AnalysisResult.
     */
    @Test
    void shouldNotContainQualityGateResult() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 0, 0, Lists.immutable.empty(), 0, 0);
        when(analysisResult.getOverallResult()).thenReturn(Result.SUCCESS);
        QualityGate qualityGate = mock(QualityGate.class);
        when(qualityGate.isEnabled()).thenReturn(false);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).doesNotContainPattern(
                createWarningsLink("Quality gate: <img src=\"color\" class=\"icon-blue icon-lg\" alt=\"Success\" title=\"Success\"> Success"));
    }

    /**
     * Tests if the ReferenceBuild message with a link is included in the HTML.
     */
    @Test
    void shouldContainReferenceBuild() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 0, 0, Lists.immutable.empty(), 0, 0);
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).contains("Reference build: <a href=\"absoluteUrl\">Job #15</a>");
    }

    /**
     * Tests if no ReferenceBuild message is included in the HTML when there is none in the AnalysisResult.
     */
    @Test
    void shouldNotContainReferenceBuild() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of(), 0, 0, Lists.immutable.empty(), 0, 0);
        when(analysisResult.getReferenceBuild()).thenReturn(Optional.empty());
        String createdHtml = createTestData(analysisResult).create();
        assertThat(createdHtml).doesNotContain("Reference build:");
    }

    /**
     * Tests the creation of the html when multiple conditions are met.
     */
    @Test
    void shouldProvideSummary() {
        AnalysisResult analysisResult = createAnalysisResult(Maps.fixedSize.of("checkstyle", 15, "pmd", 20), 2, 2, Lists.immutable.empty(), 1, 1);
        Summary summary = createTestData(analysisResult);

        String actualSummary = summary.create();
        assertThat(actualSummary).contains("CheckStyle, PMD");
        assertThat(actualSummary).contains("No warnings for 2 builds");
        assertThat(actualSummary).contains("since build <a href=\"../1\" class=\"model-link inside\">1</a>");
        assertThat(actualSummary).containsPattern(
                createWarningsLink("<a href=\"testResult/new\">.*2 new warnings.*</a>"));
        assertThat(actualSummary).containsPattern(
                createWarningsLink("<a href=\"testResult/fixed\">.*2 fixed warnings.*</a>"));
        assertThat(actualSummary).contains("Quality gate: <img src=\"color\" class=\"icon-blue icon-lg\" alt=\"Success\" title=\"Success\"> Success");
        assertThat(actualSummary).contains("Reference build: <a href=\"absoluteUrl\">Job #15</a>");
    }

    private Summary createTestData(AnalysisResult analysisResult) {
        Locale.setDefault(Locale.ENGLISH);

        LabelProviderFactoryFacade facade = mock(LabelProviderFactoryFacade.class);
        StaticAnalysisLabelProvider checkStyleLabelProvider = createLabelProvider("checkstyle", "CheckStyle");
        when(facade.get("checkstyle")).thenReturn(checkStyleLabelProvider);
        StaticAnalysisLabelProvider pmdLabelProvider = createLabelProvider("pmd", "PMD");
        when(facade.get("pmd")).thenReturn(pmdLabelProvider);

        return new Summary(createLabelProvider("test", "SummaryTest"), analysisResult, facade);
    }


    private AnalysisResult createAnalysisResult(Map sizesPerOrigin, int newSize, int fixedSize, ImmutableList<String> errorMessages,
                                                int numberOfIssuesSinceBuild, int numberOfThreshholds) {
        AnalysisResult analysisRun = mock(AnalysisResult.class);
        when(analysisRun.getSizePerOrigin()).thenReturn(sizesPerOrigin);
        when(analysisRun.getNewSize()).thenReturn(newSize);
        when(analysisRun.getFixedSize()).thenReturn(fixedSize);
        when(analysisRun.getErrorMessages()).thenReturn(errorMessages);
        when(analysisRun.getNoIssuesSinceBuild()).thenReturn(numberOfIssuesSinceBuild);

        Thresholds thresholds = new Thresholds();
        thresholds.unstableTotalAll = numberOfThreshholds;
        when(analysisRun.getQualityGate()).thenReturn(new QualityGate(thresholds));
        when(analysisRun.getOverallResult()).thenReturn(Result.SUCCESS);
        Run<?, ?> run = mock(Run.class);
        when(run.getFullDisplayName()).thenReturn("Job #15");
        when(run.getUrl()).thenReturn("job/my-job/15");
        when(analysisRun.getReferenceBuild()).thenReturn(Optional.of(run));

        AnalysisBuild build = mock(AnalysisBuild.class);
        when(build.getNumber()).thenReturn(2);
        when(analysisRun.getBuild()).thenReturn(build);

        return analysisRun;
    }

    private StaticAnalysisLabelProvider createLabelProvider(final String checkstyle, final String checkStyle) {
        JenkinsFacade jenkins = mock(JenkinsFacade.class);
        when(jenkins.getImagePath(any())).thenReturn("color");
        when(jenkins.getAbsoluteUrl(any())).thenReturn("absoluteUrl");
        return new StaticAnalysisLabelProvider(checkstyle, checkStyle, jenkins);
    }

    private Pattern createWarningsLink(final String href) {
        return Pattern.compile(href, Pattern.MULTILINE | Pattern.DOTALL);
    }
}