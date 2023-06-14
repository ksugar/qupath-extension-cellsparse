package org.elephant.cellsparse;


public class CellsparseResetBody {

	@SuppressWarnings("unused")
	private String modelname;
	
	public CellsparseResetBody(final Builder builder) {
		this.modelname = builder.modelname;
	}
	
	static class Builder {
		private String modelname;
		
		public Builder(final String modelname) {
			this.modelname = modelname;
		};
		
		public CellsparseResetBody build() {
			return new CellsparseResetBody(this);
		}
	}
	
	public static CellsparseResetBody.Builder newBuilder(final String modelname) {
		return new Builder(modelname);
	}
}
