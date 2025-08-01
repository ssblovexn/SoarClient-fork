package com.soarclient.libraries.flac.frame;

/**
 * libFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2001,2002,2003  Josh Coalson
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 */

/**
 * BadHeaderException.
 * 
 * @author kc7bfi
 */
public class BadHeaderException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Exception constructor.
	 */
	public BadHeaderException() {
		super();
	}

	/**
	 * Exception constructor.
	 * 
	 * @param msg The exception message
	 */
	public BadHeaderException(String msg) {
		super(msg);
	}

	/**
	 * Exception constructor.
	 * 
	 * @param err The throwable error
	 */
	public BadHeaderException(Throwable err) {
		super(err);
	}

	/**
	 * Exception constructor.
	 * 
	 * @param msg The exception message
	 * @param err The throwable error
	 */
	public BadHeaderException(String msg, Throwable err) {
		super(msg, err);
	}

}
