/** BSD 2-Clause License:
 *  Copyright (c) 2010
 *  Michael Eichberg (Software Engineering)
 *  Department of Computer Science
 *  Technische Universität Darmstadt
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Engineering Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package apps.flashcards;

import static apps.flashcards.ui.Utilities.createImageIcon;
import static java.awt.Toolkit.getDefaultToolkit;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

import java.awt.Frame;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


import apps.flashcards.ui.Utilities;


/**
 * This class provides the major part of the Mac OS X integration of the flashcards app.
 * <p>
 * <i> This class does not introduce any coupling on Mac OS X specific classes or technologies. </i>
 * </p>
 *
 * @author Michael Eichberg
 */
public class MacOSXIntegration {

	static {

		// Properties to make the application look more like a native Mac OS X application.
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Flashcards");

		try {
			Class<?> appClass = Class.forName("com.apple.eawt.Application");
			Object app = appClass.getMethod("getApplication").invoke(null);
			appClass.getMethod("setEnabledPreferencesMenu", boolean.class).invoke(app, Boolean.FALSE);

			Image appImage = getDefaultToolkit().getImage(
					Utilities.class.getResource("Papers-icon.png"));
			appClass.getMethod("setDockIconImage", Image.class).invoke(app, appImage);

			Class<?> appAdapterClass = Class.forName("com.apple.eawt.ApplicationListener");
			Object appAdapter = Proxy.newProxyInstance(System.class.getClassLoader(), new Class<?>[] {
				appAdapterClass
			}, new InvocationHandler() {

				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

					if (method.getName().equals("handleAbout")) {
						showMessageDialog(
								null,
								"(c) 2010 Michael Eichberg,\nDepartment of Computer Science,\n"
										+ "Technische Universität Darmstadt",
								"Flashcards 0.4",
								INFORMATION_MESSAGE,
								createImageIcon("Papers-icon.png", "The Flashcards Icon"));
						args[0].getClass().getMethod("setHandled", boolean.class).invoke(args[0], Boolean.TRUE);
					} else if (method.getName().equals("handleQuit")) {
						Boolean handled = Boolean.TRUE;
						for (Frame window : Frame.getFrames()) {
							if (window.isShowing()) {
								window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
								if (window.isShowing()) {
									handled = Boolean.FALSE;
								}
							}
						}
						args[0]
								.getClass()
								.getMethod("setHandled", boolean.class)
								.invoke(args[0], handled);
					}
					return null;
				}
			});

			appClass.getMethod(
					"addApplicationListener",
					Class.forName("com.apple.eawt.ApplicationListener")).invoke(app, appAdapter);
		} catch (Exception e) {
			System.err.println("Mac OS X integration failed: " + e.getLocalizedMessage() + ".");
		}
	}
}
