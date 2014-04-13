package rubberband.math;
//Matrix.java
//solve, invert, etc. last argument is output
//void solve(double A[][], double Y[], double X[]);          X=A^-1*Y
//void invert(double A[][]);                                 A=A^-1
//double determinant(double A[]);                            d=det A
//void eigenvalues(double A[][], double V[][], double Y[]);  V,Y=eigen A
//void checkeigen(double A[][], V[][], double Y[]);          printout
//void multiply(double A[][], double B[][], double C[][]);   C=A*B
//void add(double A[][], double B[][], double C[][]);        C=A+B
//void subtract(double C[][], double A[][], double B[][]);   C=A-B
//double norm1(double A[][]);                                d=norm1 A
//double norm2(double A[][]);  sqrt largest eigenvalue A^T*A d=norm2 A
//double normFro(double A[][]); Frobenius                    d=normFro A
//double normInf(double A[][]);                              d=normInf A
//void identity(double A[][]);                               A=I
//void zero(double A[][]);                                   A=0
//void copy(double A[][], doubleB[][]);                      B=A
//void boolean equals(double A[][], doubleB[][]);            B==A
//void print(double A[][]);                                  A
//void multiply(double A[][], double X[], double Y[]);       Y=A*X
//void add(double X[], double Y[], double Z[]);              Z=X+Y
//void subtract(double X[], double Y[], double Z[]);         Z=X-Y
//double norm1(double X[]);                                  d=norm1 X
//double norm2(double X[]);                                  d=norm2 X
//double normInf(double X[]);                                d=normInf X
//void unitVector(double X[], int i);                        X[i]=1 else 0
//void zero(double X[]);                                     X=0
//void copy(double X[], double Y[]);                         Y=X
//void boolean equals(double X[], doubleY[]);                X==Y
//void print(double X[]);                                    X


public strictfp class Matrix
{

public static void solve(final double A[][], final double Y[],
                        double X[])
{
 // solve real linear equations for X where Y = A * X
 // method: Gauss-Jordan elimination using maximum pivot
 // usage:  Matrix.solve(A,Y,X);
 //    Translated to java by : Jon Squire , 26 March 2003
 //    First written by Jon Squire December 1959 for IBM 650, translated to
 //    other languages  e.g. Fortran converted to Ada converted to C
 //    converted to java
 int n=A.length;
 int m=n+1;
 double B[][]=new double[n][m];  // working matrix
 int row[]=new int[n];           // row interchange indicies
 int hold , I_pivot;             // pivot indicies
 double pivot;                   // pivot element value
 double abs_pivot;
 if(A[0].length!=n || Y.length!=n || X.length!=n)
 {
   System.out.println("Error in Matrix.solve, inconsistent array sizes.");
 }
 // build working data structure
 for(int i=0; i<n; i++)
 {
   for(int j=0; j<n; j++)
   {
     B[i][j] = A[i][j];
   }
   B[i][n] = Y[i];
 }
 // set up row interchange vectors
 for(int k=0; k<n; k++)
 {
   row[k] = k;
 }
 //  begin main reduction loop
 for(int k=0; k<n; k++)
 {
   // find largest element for pivot
   pivot = B[row[k]][k];
   abs_pivot = Math.abs(pivot);
   I_pivot = k;
   for(int i=k; i<n; i++)
   {
     if(Math.abs(B[row[i]][k]) > abs_pivot)
     {
       I_pivot = i;
       pivot = B[row[i]][k];
       abs_pivot = Math.abs(pivot);
     }
   }
   // have pivot, interchange row indicies
   hold = row[k];
   row[k] = row[I_pivot];
   row[I_pivot] = hold;
   // check for near singular
   if(abs_pivot < 1.0E-10)
   {
     for(int j=k+1; j<n+1; j++)
     {
       B[row[k]][j] = 0.0;
     }
     System.out.println("redundant row (singular) "+row[k]);
   } // singular, delete row
   else
   {
     // reduce about pivot
     for(int j=k+1; j<n+1; j++)
     {
       B[row[k]][j] = B[row[k]][j] / B[row[k]][k];
     }
     //  inner reduction loop
     for(int i=0; i<n; i++)
     {
       if( i != k)
       {
         for(int j=k+1; j<n+1; j++)
         {
           B[row[i]][j] = B[row[i]][j] - B[row[i]][k] * B[row[k]][j];
         }
       }
     }
   }
   //  finished inner reduction
 }
 //  end main reduction loop
 //  build  X  for return, unscrambling rows
 for(int i=0; i<n; i++)
 {
   X[i] = B[row[i]][n];
 }
} // end solve

public static final void invert(double A[][])
{
 int n = A.length;
 int row[] = new int[n];
 int col[] = new int[n];
 double temp[] = new double[n];
 int hold , I_pivot , J_pivot;
 double pivot, abs_pivot;

 if(A[0].length!=n)
 {
   System.out.println("Error in Matrix.invert, inconsistent array sizes.");
 }
 // set up row and column interchange vectors
 for(int k=0; k<n; k++)
 {
   row[k] = k ;
   col[k] = k ;
 }
 // begin main reduction loop
 for(int k=0; k<n; k++)
 {
   // find largest element for pivot
   pivot = A[row[k]][col[k]] ;
   I_pivot = k;
   J_pivot = k;
   for(int i=k; i<n; i++)
   {
     for(int j=k; j<n; j++)
     {
       abs_pivot = Math.abs(pivot) ;
       if(Math.abs(A[row[i]][col[j]]) > abs_pivot)
       {
         I_pivot = i ;
         J_pivot = j ;
         pivot = A[row[i]][col[j]] ;
       }
     }
   }
   if(Math.abs(pivot) < 1.0E-10)
   {
     System.out.println("Matrix is singular !");
     return;
   }
   hold = row[k];
   row[k]= row[I_pivot];
   row[I_pivot] = hold ;
   hold = col[k];
   col[k]= col[J_pivot];
   col[J_pivot] = hold ;
    // reduce about pivot
   A[row[k]][col[k]] = 1.0 / pivot ;
   for(int j=0; j<n; j++)
   {
     if(j != k)
     {
       A[row[k]][col[j]] = A[row[k]][col[j]] * A[row[k]][col[k]];
     }
   }
   // inner reduction loop
   for(int i=0; i<n; i++)
   {
     if(k != i)
     {
       for(int j=0; j<n; j++)
       {
         if( k != j )
         {
           A[row[i]][col[j]] = A[row[i]][col[j]] - A[row[i]][col[k]] *
                                A[row[k]][col[j]] ;
         }
       }
       A[row[i]][col [k]] = - A[row[i]][col[k]] * A[row[k]][col[k]] ;
     }
   }
 }
 // end main reduction loop

 // unscramble rows
 for(int j=0; j<n; j++)
 {
   for(int i=0; i<n; i++)
   {
     temp[col[i]] = A[row[i]][j];
   }
   for(int i=0; i<n; i++)
   {
     A[i][j] = temp[i] ;
   }
 }
 // unscramble columns
 for(int i=0; i<n; i++)
 {
   for(int j=0; j<n; j++)
   {
     temp[row[j]] = A[i][col[j]] ;
   }
   for(int j=0; j<n; j++)
   {
     A[i][j] = temp[j] ;
   }
 }
} // end invert

public static final double determinant(final double A[][])
{
 int n=A.length;
 double D = 1.0;                 // determinant
 double B[][]=new double[n][n];  // working matrix
 int row[]=new int[n];             // row interchange indicies
 int hold , I_pivot;             // pivot indicies
 double pivot;                   // pivot element value
 double abs_pivot;

 if(A[0].length!=n)
 {
   System.out.println("Error in Matrix.determinant, inconsistent array sizes.");
 }
 // build working matrix
 for(int i=0; i<n; i++)
   for(int j=0; j<n; j++)
     B[i][j]=A[i][j];
 // set up row interchange vectors
 for(int k=0; k<n; k++)
 {
   row[k]= k;
 }
 // begin main reduction loop
 for(int k=0; k<n-1; k++)
 {
   // find largest element for pivot
   pivot = B[row[k]][k];
   abs_pivot = Math.abs(pivot);
   I_pivot = k;
   for(int i=k; i<n; i++)
   {
     if( Math.abs(B[row[i]][k]) > abs_pivot )
     {
       I_pivot = i;
       pivot = B[row[i]][k];
       abs_pivot = Math.abs(pivot);
     }
   }
   // have pivot, interchange row indicies
   if(I_pivot != k)
   {
     hold = row[k];
     row[k] = row[I_pivot];
     row[I_pivot] = hold;
     D = - D;
   }
   // check for near singular
   if(abs_pivot < 1.0E-10)
   {
     return 0.0;
   }
   else
   {
     D = D * pivot;
     // reduce about pivot
     for(int j=k+1; j<n; j++)
     {
       B[row[k]][j] = B[row[k]][j] / B[row[k]][k];
     }
     //  inner reduction loop
     for(int i=0; i<n; i++)
     {
       if(i != k)
       {
         for(int j=k+1; j<n; j++)
         {
           B[row[i]][j] = B[row[i]][j] - B[row[i]][k]* B[row[k]][j];
         }
       }
     }
   }
   //  finished inner reduction
 }
 // end of main reduction loop
 return D * B[row[n-1]][n-1];
} // end determinant

public static final void eigenvalues(final double A[][],
                                    double V[][], double Y[])
{
 // cyclic Jacobi iterative method of finding eigenvalues
 // advertized for symmetric real
 int n=A.length;
 double AA[][] = new double[n][n];
 double norm;
 double c[] = new double[1];
 double s[] = new double[1];
 if(A[0].length!=n || V.length!=n || V[0].length!=n || Y.length!=n)
 {
   System.out.println("Error in Matrix.eigenvalues, inconsistent array sizes.");
 }
 c[0] = 1.0;
 s[0] = 0.0;
 for(int i=0; i<n; i++) // start V as identity matrix
 {
   for(int j=0; j<n; j++) V[i][j]=0.0;
   V[i][i]=1.0;
 }
 copy(A, AA);
 for(int k=0; k<n; k++)
 {

   norm=norm4(AA);
   for(int i=0; i<n-1; i++)
   {
     for(int j=i+1; j<n; j++)
     {
       schur2(AA, i, j, c, s);
       mat44(i, j, c, s, AA, V);
     }
   } // end one iteration
 }
 norm = norm4(AA); // final quality check if desired
 for(int i=0; i<n; i++) // copy eigenvalues back to caller
   Y[i] = AA[i][i];
} // end eigenvalues

public static final void eigenCheck(final double A[][], final double V[][],
                                   double Y[])
{
 // check  A * X = lambda X  lambda=Y[i] X=V[i]
 int n=A.length;
 double X[] = new double[n];
 double Z[] = new double[n];
 double T[] = new double[n];
 double norm;

 if(A[0].length!=n || V.length!=n || V[0].length!=n || Y.length!=n)
 {
   System.out.println("Error in Matrix.eigenCheck, inconsistent array sizes.");
 }
 for(int i=0; i<n; i++)
 {
   for(int j=0; j<n; j++)
   {
     X[j]=V[j][i];
   }
   multiply(A, X, T);
   for(int j=0; j<n; j++)
   {
     Z[j]=T[j]-Y[i]*X[j];
   }
   norm = norm2(Z);
   System.out.println("check for near zero norm of Z["+i+"]="+Z[i]);
 }
} // end eigenCheck

static void schur2(final double A[][], final int p, final int q,
                  double c[], double s[])
{
 double tau;
 double t;

 if(A[0].length!=A.length || c.length!=1 || s.length!=1)
 {
   System.out.println("Error in schur2 of jacobi, inconsistent array sizes.");
 }
 if(A[p][q]!=0.0)
 {
   tau=(A[q][q]-A[p][p])/(2.0*A[p][q]);
   if(tau>=0.0)
      t=1.0/(tau+Math.sqrt(1.0+tau*tau));
   else
      t=-1.0/((-tau)+Math.sqrt(1.0+tau*tau));
   c[0]=1.0/Math.sqrt(1.0+t*t);
   s[0]=t * c[0];
 }
 else
 {
   c[0]=1.0;
   s[0]=0.0;
 }
} // end schur2

static void mat22(final double c[], final double s[], final double A[][],
                 double B[][])
{
 if(A.length!=2 || A[0].length!=2 || B.length!=2 || B[0].length!=2)
 {
   System.out.println("Error in mat22 of Jacobi, not both 2 by 2");
 }
 double T[][] = new double[2][2];

 T[0][0] = c[0] * A[0][0] - s[0] * A[0][1] ;
 T[0][1] = s[0] * A[0][0] + c[0] * A[0][1] ;
 T[1][0] = c[0] * A[1][0] - s[0] * A[1][1] ;
 T[1][1] = s[0] * A[1][0] + c[0] * A[1][1] ;

 B[0][0] = c[0] * T[0][0] - s[0] * T[1][0] ;
 B[0][1] = c[0] * T[0][1] - s[0] * T[1][1] ;
 B[1][0] = s[0] * T[0][0] + c[0] * T[1][0] ;
 B[1][1] = s[0] * T[0][1] + c[0] * T[1][1] ;
} // end mat2

static void mat44(final int p, final int q, final double c[], final double s[],
                 final double A[][], double V[][])
{
 int n = A.length;
 double B[][] = new double[n][n];
 double J[][] = new double[n][n];
 if(s.length!=1 || c.length!=1)
 {
   System.out.println("Error in mat44 of Jacobi, s or c not length 1");
 }
 if(A[0].length!=n || V.length!=n || V[0].length!=n)
 {
   System.out.println("Error in mat44 of Jacobi, A or V not same and square");
 }
 for(int i=0; i<n; i++)
 {
   for(int j=0; j<n; j++)
   {
     J[i][j]=0.0;
   }
   J[i][i]=1.0;
 }
 J[p][p]=c[0]; /* J transpose */
 J[p][q]=-s[0];
 J[q][q]=c[0];
 J[q][p]=s[0];
 multiply(J, A, B);
 J[p][q]=s[0];
 J[q][p]=-s[0];
 multiply(B, J, A);
 multiply(V, J, B);
 copy(B, V);
} // end mat44

static double norm4(final double A[][]) // for Jacobi
{
 int n=A.length;
 int nr=A[0].length;
 double nrm=0.0;
 if(n!=nr)
 {
   System.out.println("Error in norm4, non square A["+n+"]["+nr+"]");
 }
 for(int i=0; i<n-1; i++)
 {
   for(int j=i+1; j<n; j++)
   {
     nrm=nrm+Math.abs(A[i][j])+Math.abs(A[j][i]);
   }
 }
 return nrm/(n*n-n);
} // end norm4

public static final void multiply(final double A[][], final double B[][],
                                 double C[][])
{
 int ni = A.length;
 int nk = A[0].length;
 int nj = B[0].length;
 if(B.length!=nk || C.length!=ni || C[0].length!=nj)
 {
   System.out.println("Error in Matrix.multiply, incompatible sizes");
 }
 for(int i=0; i<ni; i++)
   for(int j=0; j<nj; j++)
   {
     C[i][j] = 0.0;
     for(int k=0; k<nk; k++)
       C[i][j] = C[i][j] + A[i][k] * B[k][j];
   }
} // end multiply

public static final void add(final double A[][], final double B[][],
                            double C[][])
{
 int ni=A.length;
 int nj=A[0].length;
 if(B.length!=ni || C.length!=ni || B[0].length!=nj || C[0].length!=nj)
 {
   System.out.println("Error in Matrix.add, incompatible sizes");
 }
 for(int i=0; i<ni; i++)
   for(int j=0; j<nj; j++)
     C[i][j] = A[i][j] + B[i][j];
} // end add

public static final void subtract(final double A[][], final double B[][], double C[][])
{
 int ni=A.length;
 int nj=A[0].length;
 if(B.length!=ni || C.length!=ni || B[0].length!=nj || C[0].length!=nj)
 {
   System.out.println("Error in Matrix.subtract, incompatible sizes");
 }
 for(int i=0; i<ni; i++)
   for(int j=0; j<nj; j++)
     C[i][j] = A[i][j] - B[i][j];
} // end subtract

public static final double norm1(final double A[][])
{
 double norm=0.0;
 double colSum;
 int ni=A.length;
 int nj=A[0].length;
 for(int j=0; j<nj; j++)
 {
   colSum = 0.0;
   for(int i=0; i<ni; i++)
     colSum = colSum + Math.abs(A[i][j]);
   norm = Math.max(norm, colSum);
 }
 return norm;
} // end norm1

public static final double normInf(final double A[][])
{
 double norm=0.0;
 double rowSum;
 int ni=A.length;
 int nj=A[0].length;
 for(int i=0; i<ni; i++)
 {
   rowSum = 0.0;
   for(int j=0; j<nj; j++)
     rowSum = rowSum + Math.abs(A[i][j]);
   norm = Math.max(norm, rowSum);
 }
 return norm;
} // end normInf

public static final void identity(double A[][])
{
 int n=A.length;
 if(A[0].length!=n)
 {
   System.out.println("Error in Matrix.identity, not square");
 }
 for(int i=0; i<n; i++)
 {
   for(int j=0; j<n; j++) A[i][j]=0.0;
   A[i][i]=1.0;
 }
} // end identity

public static final void zero(double A[][])
{
 int ni=A.length;
 int nj=A[0].length;
 for(int i=0; i<ni; i++)
   for(int j=0; j<nj; j++) A[i][j]=0.0;
} // end zero

public static final double normFro(final double A[][])
{
 double norm=0.0;
 int n=A.length;
 for(int i=0; i<n; i++)
   for(int j=0; j<n; j++)
     norm = norm + A[i][j]*A[i][j];
 return Math.sqrt(norm);
} // end normFro


public static final double norm2(final double A[][])
{
 double r=0.0; // largest eigenvalue
 int n=A.length;
 double B[][] = new double[n][n];
 double V[][] = new double[n][n];
 double BI[] = new double[n];
 for(int i=0; i<n; i++)  // B = A^T * A
 {
   for(int j=0; j<n; j++)
   {
     B[i][j]=0.0;
     for(int k=0; k<n; k++)
       B[i][j] = B[i][j] + A[k][i]*A[k][j];
   }
 }
 eigenvalues(B, V, BI);
 for(int i=0; i<n; i++) r=Math.max(r,BI[i]);
 return Math.sqrt(r);
} // end norm2

public static final void copy(final double A[][], double B[][])
{
 int ni = A.length;
 int nj = A[0].length;
 if(B.length!=ni || B[0].length!=nj)
 {
	System.out.println("Error in Matrix.copy,"+
                        " incompatible sizes.");
 }
 for(int i=0; i<ni; i++)
   for(int j=0; j<nj; j++)
     B[i][j] = A[i][j];
} // end copy

public static final boolean equals(final double A[][], final double B[][])
{
 int ni = A.length;
 int nj = A[0].length;
 boolean same = true;
 if(B.length!=ni || B[0].length!=nj)
 {
	System.out.println("Error in Matrix.equals,"+
                        " incompatible sizes.");
 }
 for(int i=0; i<ni; i++)
   for(int j=0; j<nj; j++)
     same = same && (A[i][j] == B[i][j]);
 return same;
} // end equals

public static final void print(double A[][])
{
 int N = A.length;
 for(int i=0; i<N; i++)
   for(int j=0; j<N; j++)
     System.out.println("A["+i+"]["+j+"]="+A[i][j]);
} // end print

public static final void multiply(double A[][], double B[], double C[])
{
 int n=A.length;
 for(int i=0; i<n; i++){
   C[i] = 0.0;
   for(int j=0; j<n; j++){
     C[i] = C[i] + A[i][j] * B[j];
   }
 }
} // end multiply

public static final void add(double X[], double Y[], double Z[])
{
 int n=X.length;
 if(Y.length!=n || Z.length!=n)
 {
   System.out.println("Error in Matrix.add,"+
                      " incompatible sizes.");
 }
 for(int i=0; i<n; i++) Z[i] = X[i] + Y[i];
} // end add

public static final void subtract(double X[], double Y[], double Z[])
{
 int n=X.length;
 if(Y.length!=n || Z.length!=n)
 {
   System.out.println("Error in Matrix.subtract,"+
                      " incompatible sizes.");
 }
 for(int i=0; i<n; i++) Z[i] = X[i] - Y[i];
} // end subtract

public static final double norm1(double X[])
{
 double norm=0.0;
 int n=X.length;
 for(int i=0; i<n; i++)
     norm = norm + Math.abs(X[i]);
 return norm;
} // end norm1

public static final double norm2(double X[])
{
 double norm=0.0;
 int n=X.length;
 for(int i=0; i<n; i++)
     norm = norm + X[i]*X[i];
 return Math.sqrt(norm);
} // end norm2

public static final double normInf(double X[])
{
 double norm=0.0;
 int n=X.length;
 for(int i=0; i<n; i++)
     norm = Math.max(norm, Math.abs(X[i]));
 return norm;
} // end normInf

public static final void unitVector(double X[], int j)
{
 int n=X.length;
 for(int i=0; i<n; i++) X[i]=0.0;
 X[j]=1.0;
} // end unitVector

public static final void zero(double X[])
{
 int n=X.length;
 for(int i=0; i<n; i++) X[i]=0.0;
} // end zero

public static final void copy(double X[], double Y[])
{
 int n = X.length;
 if(Y.length!=n)
 {
   System.out.println("Error in Matrix.copy,"+
                      " incompatible sizes.");
 }
 for(int i=0; i<n; i++) Y[i] = X[i];
} // end copy

public static final boolean equals(final double X[], final double Y[])
{
 int n = X.length;
 boolean same = true;
 if(Y.length!=n)
 {
	System.out.println("Error in Matrix.equals,"+
                        " incompatible sizes.");
 }
 for(int i=0; i<n; i++)
   same = same && (X[i] == Y[i]);
 return same;
} // end equals

public static final void print(double X[])
{
 int n = X.length;
 for(int i=0; i<n; i++)
   System.out.println("X["+i+"]="+X[i]);
} // end print

} // end class Matrix