package com.cnp.mybtcar;

import android.graphics.Bitmap;

/**
 * @author Micho Garcia
 * 
 */
public interface ImjpegViewListener {

	void sucess();

	void error();

	void hasBitmap(Bitmap bm);
}