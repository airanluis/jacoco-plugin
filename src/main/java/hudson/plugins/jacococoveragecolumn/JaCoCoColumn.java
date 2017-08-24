package hudson.plugins.jacococoveragecolumn;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.jacoco.JacocoBuildAction;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * View column that shows the code coverage (line) percentage
 *
 */
public class JaCoCoColumn extends ListViewColumn {

	@DataBoundConstructor
	public JaCoCoColumn() {
	}

	public boolean hasCoverage(final Job<?, ?> job) {
		final Run<?, ?> lastSuccessfulBuild = job.getLastSuccessfulBuild();
		if (lastSuccessfulBuild == null) {
			return false;
		} else if (lastSuccessfulBuild.getAction(JacocoBuildAction.class) == null){
			return false;
		}

		return true;
	}

	public String getPercent(final Job<?, ?> job) {
		final StringBuilder stringBuilder = new StringBuilder();

		if (!hasCoverage(job)) {
			stringBuilder.append("N/A");
		} else {
			final Run<?, ?> lastSuccessfulBuild = job.getLastSuccessfulBuild();
			final Double percent = getOverallPercent(lastSuccessfulBuild);
			stringBuilder.append(percent);
		}

		return stringBuilder.toString();
	}

	public String getLineColor(final Job<?, ?> job, final BigDecimal amount) {
		if (amount == null) {
			return null;
		}

		if(job != null && !hasCoverage(job)) {
			return CoverageRange.NA.getLineHexString();
		}

		return CoverageRange.valueOf(amount.doubleValue()).getLineHexString();
	}

	public String getFillColor(final Job<?, ?> job, final BigDecimal amount) {
		if (amount == null) {
			return null;
		}

		if(job != null && !hasCoverage(job)) {
			return CoverageRange.NA.getFillHexString();
		}

		final Color c = CoverageRange.fillColorOf(amount.doubleValue());
		return CoverageRange.colorAsHexString(c);
	}

	public BigDecimal getOverallCoverage(final Job<?, ?> job) {
		final Run<?, ?> lastSuccessfulBuild = job.getLastSuccessfulBuild();
		return BigDecimal.valueOf(getOverallPercent(lastSuccessfulBuild));
	}

	private Double getOverallPercent(final Run<?, ?> lastSuccessfulBuild) {
		final Float percentageFloat = getPercentageFloat(lastSuccessfulBuild);
		final double doubleValue = percentageFloat.doubleValue();

		final int decimalPlaces = 2;
		BigDecimal bigDecimal = new BigDecimal(doubleValue);

		// setScale is immutable
		bigDecimal = bigDecimal.setScale(decimalPlaces,
				RoundingMode.HALF_UP);
		return bigDecimal.doubleValue();
	}

	private Float getPercentageFloat(final Run<?, ?> lastSuccessfulBuild) {
		if(lastSuccessfulBuild == null) {
			return 0f;
		}

		final JacocoBuildAction action = lastSuccessfulBuild
				.getAction(JacocoBuildAction.class);

		if(action == null) {
			return 0f;
		}

		if (!action.hasLineCoverage() || action.getBranchCoverage().getTotal() == 0) {
			return 100f;
		}

		final double branch_dividend = action.getBranchCoverage().getPercentageFloat() * 0.01 * action.getBranchCoverage().getTotal();
		final double dividend = branch_dividend + action.getLineCoverage().getCovered();
		final double divisor = action.getBranchCoverage().getTotal() + action.getLineCoverage().getTotal();

		return (float) (dividend / divisor) * 100;
	}

	@Extension
	public static final Descriptor<ListViewColumn> DESCRIPTOR = new DescriptorImpl();

	@Override
	public Descriptor<ListViewColumn> getDescriptor() {
		return DESCRIPTOR;
	}

	private static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public ListViewColumn newInstance(final StaplerRequest req,
				final JSONObject formData) throws FormException {
			return new JaCoCoColumn();
		}

		@Override
		public boolean shownByDefault() {
			return false;
		}

		@Override
		public String getDisplayName() {
			return "JaCoCo Overall Coverage";
		}
	}
}
