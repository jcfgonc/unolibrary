package structures;

import java.lang.reflect.Array;

/******************************************************************************
 * Compilation: javac Matrix.java Execution: java Matrix
 *
 * A bare-bones immutable data type for M-by-N matrices.
 *
 *
 * 
 * Copyright © 2000–2011, Robert Sedgewick and Kevin Wayne. Last updated: Sun Aug 2 18:43:37 EDT 2015.
 ******************************************************************************/

final public class Matrix<E> {
	private int M; // number of rows
	private int N; // number of columns
	private E[][] data; // M-by-N array
	private Class<E> c;

	// create M-by-N matrix of 0's
	@SuppressWarnings("unchecked")
	public Matrix(int M, int N, Class<E> c) {
		this.M = M;
		this.N = N;
		this.c = c;
		this.data = (E[][]) Array.newInstance(c, M, N);
	}

	// does A = B exactly?
	public boolean eq(Matrix<E> B) {
		Matrix<E> A = this;
		if (B.M != A.M || B.N != A.N)
			throw new RuntimeException("Illegal matrix dimensions.");
		for (int i = 0; i < M; i++)
			for (int j = 0; j < N; j++)
				if (A.data[i][j] != B.data[i][j])
					return false;
		return true;
	}

	public E getElement(int row, int col) {
		return this.data[row][col];
	}

	public void setElement(int row, int col, E value) {
		this.data[row][col] = value;
	}

	// print matrix to standard output
	public void show() {
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++)
				System.out.printf("%s ", data[i][j]);
			System.out.println();
		}
	}

	// swap rows i and j
	public void swap(int i, int j) {
		E[] temp = data[i];
		data[i] = data[j];
		data[j] = temp;
	}

	// create and return the transpose of the invoking matrix
	public Matrix<E> transpose() {
		Matrix<E> A = new Matrix<E>(N, M, c);
		for (int i = 0; i < M; i++)
			for (int j = 0; j < N; j++)
				A.data[j][i] = this.data[i][j];
		return A;
	}

}
