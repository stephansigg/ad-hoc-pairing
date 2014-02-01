/* Copyright Florian Schweitzer
 * File created 04.12.2012
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */
package org.authentication.ambientaudio;

import java.net.Socket;

/**
 * Interface for informing host of the ambient audio authentication result
 * @author Florian Schweitzer
 *
 */
public interface OnAmbientAudioResultListener {
	
	/**
	 * Session key could be generated successfully
	 * @param key		generated session key
	 * @param remote	the remote partner
	 */
	public void onSessionKeyGeneratedSuccess(byte[] key, Socket remote);
	
	/**
	 * Session key could not be generated
	 * @param remote	the remote partner
	 * @param e			the exception which occured
	 */
	public void onSessionKeyGeneratedFailure(Socket remote, Exception e);
}
