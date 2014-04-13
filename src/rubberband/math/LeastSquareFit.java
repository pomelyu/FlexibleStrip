package rubberband.math;


//LeastSquareFit.java
//
//Given a set of pairs x[i], y[i] for y[i]=F(x[i]) i=0,n-1
//Find the least square fit polynomial coefficients c[i]
//of polynomial P(x)=c[0]+c[1]*x+c[2]*x^3+c[3]*x^3+...
//that minimize sum( (y[i]-P(x[i]))^2 )
//
//Method: build matrix A, vector Y and solve for vector C
//A*C=Y  where all sum's are over i=0,n-1 the x,y points
//r is the order of the approximating polynomial, max(r)=n
//
//  | sum(1.0)      sum(x[i]^1) sum(x[i]^2)   ... sum(x[i]^r-1)  |
//  | sum(x[i]^1)   sum(x[i]^2) sum(x[i]^3)   ... sum(x[i]^r)    |
//A = | sum(x[i]^2)   sum(x[i]^3) sum(x[i]^4)   ... sum(x[i]^r+1   |
//  | ...           ...         ...           ... ...            |
//  | sum(x[i]^r-1) sum(x[i]^r) sum(x[i]^r+1) ... sum(x[i]^2r-1) |
//
//  | sum(y[i])            |
//  | sum(x[i]^1 * y[i])   |
//Y = | sum(x[i]^2 * y[i])   |
//  | ...                  |
//  | sum(x[i]^r-1 * y[i]) |

public strictfp class LeastSquareFit {
	double c[]; // the coefficients of the fit

	public LeastSquareFit(double x[], double y[]) // constructs c's
	{
		int n = x.length;
		c = new double[n + 1];
		if (y.length != n) {
			System.out.println("Error in L.S.Fit inconsistent lengths.");
		}
		LSFit(x, y, n);
	}

	public LeastSquareFit(double x[], double y[], int order) // constructs c's
	{
		int n = x.length;
		if (y.length != n) {
			System.out.println("Error in L.S.Fit inconsistent lengths.");
		}
		if (order > n)
			order = n; // local copy of order
		c = new double[order + 1];
		LSFit(x, y, order);
	}

	private void LSFit(double x[], double y[], int order) {
		int n = x.length;
		double A[][] = new double[order + 1][order + 1];
		A[0][0] = (double) n;
		double Y[] = new double[order + 1];
		Y[0] = vecSum(y);
		double XP[] = new double[n];
		double sum;
		int i;

		Matrix.copy(x, XP);
		for (int k = 0; k <= 2 * order - 1; k++) {
			if (k < order) // compute Y[k+1]
			{
				sum = 0.0;
				for (int j = 0; j < n; j++) {
					sum = sum + XP[j] * y[j];
				}
				Y[k + 1] = sum;
			}
			sum = vecSum(XP);
			int ii = Math.max(0, k - order + 1);
			int jj = Math.min(k + 1, order);
			for (int ij = 0; ij < k + 2; ij++) {
				A[ii][jj] = sum;
				ii++;
				jj--;
				if (ii > order || jj < 0)
					break;
			}
			for (int j = 0; j < n; j++)
				XP[j] = XP[j] * x[j];
		}
		Matrix.solve(A, Y, c);
	}

	public double evaluate(double x) {
		int n = c.length;
		double val = c[n - 1];
		for (int i = n - 2; i >= 0; i--)
			val = c[i] + x * val;
		return val;
	}

	public double integrate(double xmin, double xmax) {
		int n = c.length;
		double sumMin = c[n - 1] * xmin / (double) n;
		double sumMax = c[n - 1] * xmax / (double) n;
		for (int i = n - 2; i >= 0; i--) {
			sumMin = c[i] / (double) (i + 1) + xmin * sumMin;
			sumMax = c[i] / (double) (i + 1) + xmax * sumMax;
		}
		return sumMax * xmax - sumMin * xmin;
	}

	static double vecSum(double x[]) {
		double val = 0.0;
		for (int i = 0; i < x.length; i++)
			val = val + x[i];
		return val;
	}

	static void report(double x[], double y[]) {
		System.out.println("testing LeastSquareFit");

		int n = x.length;
		for (int i = 0; i < n; i++) {
			x[i] = (double) i / 10.0;
			y[i] = Math.sin(x[i]);
		}
		
		LeastSquareFit LSF = new LeastSquareFit(x, y); // run test on class
		// measure error at given points
		System.out.println("20th order fit of 20 points");
		double Y[] = new double[n];
		double y_est;
		System.out.println("at given points: x[i], y[i], P(x[i])");
		for (int i = 0; i < n; i++) {
			y_est = LSF.evaluate(x[i]);
			Y[i] = y[i] - y_est;
			System.out.println(x[i] + ",  " + y[i] + ",  " + y_est);
		}
		System.out.println("norm2 of error = " + Matrix.norm2(Y));
		System.out.println();


		// order 10 fit
		System.out.println("Repeat using order 10 approximating polynomial");
		LSF = new LeastSquareFit(x, y, 10); // run test on class
		// measure error at given points
		System.out.println("at given points: x[i], y[i], P(x[i])");
		for (int i = 0; i < n; i++) {
			y_est = LSF.evaluate(x[i]);
			Y[i] = y[i] - y_est;
			System.out.println(x[i] + ",  " + y[i] + ",  " + y_est);
		}
		System.out.println("norm2 of error = " + Matrix.norm2(Y));
		System.out.println();		


		// order 5 fit
		System.out.println("Repeat using order 5 approximating polynomial");
		LSF = new LeastSquareFit(x, y, 5); // run test on class
		// measure error at given points
		System.out.println("at given points: x[i], y[i], P(x[i])");
		for (int i = 0; i < n; i++) {
			y_est = LSF.evaluate(x[i]);
			Y[i] = y[i] - y_est;
			System.out.println(x[i] + ",  " + y[i] + ",  " + y_est);
		}
		System.out.println("norm2 of error = " + Matrix.norm2(Y));
		System.out.println();
		
		// order 2 fit
		System.out.println("Repeat using order 2 approximating polynomial");
		LSF = new LeastSquareFit(x, y, 2); // run test on class
		// measure error at given points
		System.out.println("at given points: x[i], y[i], P(x[i])");
		for (int i = 0; i < n; i++) {
			y_est = LSF.evaluate(x[i]);
			Y[i] = y[i] - y_est;
			System.out.println(x[i] + ",  " + y[i] + ",  " + y_est);
		}
		System.out.println("norm2 of error = " + Matrix.norm2(Y));
		System.out.println();		
	}
	
	
	public static void main(String args[]) {
		System.out.println("testing LeastSquareFit");
		int n = 20;
		double x[] = new double[n];
		double y[] = new double[n];
		for (int i = 0; i < n; i++) {
			x[i] = (double) i / 10.0;
			y[i] = Math.sin(x[i]);
		}
		
		LeastSquareFit LSF = new LeastSquareFit(x, y); // run test on class
		// measure error at given points
		System.out.println("20th order fit of 20 points");
		double Y[] = new double[n];
		double y_est;
		System.out.println("at given points: x[i], y[i], P(x[i])");
		for (int i = 0; i < n; i++) {
			y_est = LSF.evaluate(x[i]);
			Y[i] = y[i] - y_est;
			System.out.println(x[i] + ",  " + y[i] + ",  " + y_est);
		}
		System.out.println("norm2 of error = " + Matrix.norm2(Y));
		System.out.println();
		double x_mid;
		System.out.println("at mid points: x[i], y[i], P(x[i])");
		for (int i = 0; i < n - 1; i++) // stay inside x's
		{
			x_mid = (double) i / 10.0 + 0.05;
			y_est = LSF.evaluate(x_mid);
			Y[i] = Math.sin(x_mid) - y_est;
			System.out.println(x_mid + ",  " + Math.sin(x_mid) + ",  " + y_est);
		}
		System.out.println("norm2 of error = " + Matrix.norm2(Y));
		System.out.println("integral = "
				+ LSF.integrate(0.0, (double) (n - 1) / 10.0));
		System.out.println();

		// order 10 fit
		System.out.println("Repeat using order 10 approximating polynomial");
		LSF = new LeastSquareFit(x, y, 10); // run test on class
		// measure error at given points
		System.out.println("at given points: x[i], y[i], P(x[i])");
		for (int i = 0; i < n; i++) {
			y_est = LSF.evaluate(x[i]);
			Y[i] = y[i] - y_est;
			System.out.println(x[i] + ",  " + y[i] + ",  " + y_est);
		}
		System.out.println("norm2 of error = " + Matrix.norm2(Y));
		System.out.println();
		System.out.println("at mid points: x[i], y[i], P(x[i])");
		for (int i = 0; i < n - 1; i++) // stay inside x's
		{
			x_mid = (double) i / 10.0 + 0.05;
			y_est = LSF.evaluate(x_mid);
			Y[i] = Math.sin(x_mid) - y_est;
			System.out.println(x_mid + ",  " + Math.sin(x_mid) + ",  " + y_est);
		}
		System.out.println("norm2 of error = " + Matrix.norm2(Y));
		System.out.println("integral = "
				+ LSF.integrate(0.0, (double) (n - 1) / 10.0));
		System.out.println();

		// order 5 fit
		System.out.println("Repeat using order 5 approximating polynomial");
		LSF = new LeastSquareFit(x, y, 5); // run test on class
		// measure error at given points
		System.out.println("at given points: x[i], y[i], P(x[i])");
		for (int i = 0; i < n; i++) {
			y_est = LSF.evaluate(x[i]);
			Y[i] = y[i] - y_est;
			System.out.println(x[i] + ",  " + y[i] + ",  " + y_est);
		}
		System.out.println("norm2 of error = " + Matrix.norm2(Y));
		System.out.println();
		System.out.println("at mid points: x[i], y[i], P(x[i])");
		for (int i = 0; i < n - 1; i++) // stay inside x's
		{
			x_mid = (double) i / 10.0 + 0.05;
			y_est = LSF.evaluate(x_mid);
			Y[i] = Math.sin(x_mid) - y_est;
			System.out.println(x_mid + ",  " + Math.sin(x_mid) + ",  " + y_est);
		}
		System.out.println("norm2 of error = " + Matrix.norm2(Y));
		System.out.println("integral = " + LSF.integrate(0.0, 1.9) + "  exact="
				+ (-Math.cos(1.9) + Math.cos(0.0)));
		System.out.println();
	} // end main
} // end class LeastSquareFit