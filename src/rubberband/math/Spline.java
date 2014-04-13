package rubberband.math;

public class Spline {

	private int n;
	private float[] a, b, c, d;
	private float[] x;

	public Spline(float[] sp) {
		float[] w;
		float tmp;
		n = sp.length;

		a = new float[n];
		b = new float[n];
		c = new float[n];
		d = new float[n];
		w = new float[n];

		for (int i = 0; i < n; i++) {
			a[i] = sp[i];
		}

		c[0] = 0.0f;
		c[n - 1] = 0.0f;

		for (int i = 1; i < n - 1; i++) {
			c[i] = 3.0f * (a[i - 1] - 2.0f * a[i] + a[i + 1]);
		}

		w[0] = 0.0f;
		for (int i = 1; i < n - 1; i++) {
			tmp = 4.0f - w[i - 1];
			c[i] = (c[i] - c[i - 1]) / tmp;
			w[i] = 1.0f / tmp;
		}

		for (int i = n - 2; i > 0; i--) {
			c[i] = c[i] - c[i + 1] * w[i];
		}

		b[n - 1] = 0.0f;
		d[n - 1] = 0.0f;

		for (int i = 0; i < n - 1; i++) {
			d[i] = (c[i + 1] - c[i]) / 3.0f;
			b[i] = a[i + 1] - a[i] - c[i] - d[i];
		}

		/*
		 * for(int i=0; i<n; i++){ print("[" + i + "] "); println("a:" + a[i] +
		 * " b:" + b[i] + " c:" + c[i] + " d:" + d[i]); } println();
		 */

	}

	public float calc(float t) {
		int j;
		float dt;

		j = (int) Math.floor(t);

		if (j < 0)
			j = 0;
		else if (j > n)
			j = n;

		dt = t - (float) j;
		return a[j] + (b[j] + (c[j] + d[j] * dt) * dt) * dt;
	}

}
