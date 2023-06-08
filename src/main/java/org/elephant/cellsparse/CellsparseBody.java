package org.elephant.cellsparse;


public class CellsparseBody {

	@SuppressWarnings("unused")
	private String modelname;
	@SuppressWarnings("unused")
	private String b64img;
	@SuppressWarnings("unused")
	private String b64lbl;
	@SuppressWarnings("unused")
	private boolean train;
	@SuppressWarnings("unused")
	private boolean eval;
	@SuppressWarnings("unused")
	private int epochs;
	@SuppressWarnings("unused")
	private int batchsize;
	@SuppressWarnings("unused")
	private int steps;
	
	public CellsparseBody(final Builder builder) {
		this.modelname = builder.modelname;
		this.b64img = builder.b64img;
		this.b64lbl = builder.b64lbl;
		this.train = builder.train;
		this.eval = builder.eval;
		this.epochs = builder.epochs;
		this.batchsize = builder.batchsize;
		this.steps = builder.steps;
	}
	
	static class Builder {
		private String modelname;
		private String b64img;
		private String b64lbl = null;
		private boolean train = false;
		private boolean eval = false;
		private int epochs = 10;
		private int batchsize = 8;
		private int steps = 10;
		
		public Builder(final String modelname) {
			this.modelname = modelname;
		};
		
		public Builder b64img(final String b64img) {
			this.b64img = b64img;
			return this;
		}
		
		public Builder b64lbl(final String b64lbl) {
			this.b64lbl = b64lbl;
			return this;
		}
		
		public Builder train(final boolean train) {
			this.train = train;
			return this;
		}
		
		public Builder eval(final boolean eval) {
			this.eval = eval;
			return this;
		}
		
		public Builder epochs(final int epochs) {
			this.epochs = epochs;
			return this;
		}
		
		public Builder batchsize(final int batchsize) {
			this.batchsize = batchsize;
			return this;
		}
		
		public Builder steps(final int steps) {
			this.steps = steps;
			return this;
		}
		
		public CellsparseBody build() {
			return new CellsparseBody(this);
		}
	}
	
	public static CellsparseBody.Builder newBuilder(final String modelname) {
		return new Builder(modelname);
	}
}
