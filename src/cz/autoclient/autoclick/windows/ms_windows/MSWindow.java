/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.autoclient.autoclick.windows.ms_windows;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import cz.autoclient.autoclick.ColorRef;
import cz.autoclient.autoclick.Rect;
import cz.autoclient.autoclick.exceptions.APIException;
import cz.autoclient.autoclick.exceptions.WindowAccessDeniedException;
import cz.autoclient.autoclick.windows.MouseButton;
import cz.autoclient.autoclick.windows.Window;
import cz.autoclient.autoclick.windows.WindowValidator;
import cz.autoclient.dllinjection.NativeProcess;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import sirius.classes.Common;
import sirius.core.GDI32Ext;
import sirius.core.Psapi;
import sirius.core.User32Ext;
import sirius.core.types.WinDefExt;

/**
 *
 * @author Jakub
 */
public class MSWindow extends Common implements Window  {
  private final long hwnd_id;
  public final WinDef.HWND hwnd;
  public MSWindow(long id) {
    hwnd_id = id;
    hwnd = longToHwnd(id);
  }
  public MSWindow(WinDef.HWND handle) {
    hwnd_id = handle.hashCode();
    hwnd = handle;
  }
  
  
  @Override
  public void mouseDown(int x, int y) {
    mouseDown(x,y,MouseButton.Left, false, false, false);
  }
  @Override
  public void mouseDown(int x, int y, MouseButton button) {
    mouseDown(x,y,button, false, false, false);
  }
  @Override
  public void mouseDown(int x, int y, MouseButton button, boolean isControl, boolean isAlt, boolean isShift) {
    sendMsg(
                button.win_message_id_down,
                makeWinapiMouseEventFlags(isControl, isAlt, isShift),
                makeLParam(x, y).intValue());
  }

  @Override
  public void mouseUp(int x, int y) {
    mouseUp(x,y,MouseButton.Left, false, false, false);
  }
  @Override
  public void mouseUp(int x, int y, MouseButton button) {
    mouseUp(x,y,button, false, false, false);
  }
  @Override
  public void mouseUp(int x, int y, MouseButton button, boolean isControl, boolean isAlt, boolean isShift) {
    sendMsg(
                button.win_message_id_up,
                makeWinapiMouseEventFlags(isControl, isAlt, isShift),
                makeLParam(x, y).intValue());
  }

  @Override
  public void click(int x, int y) {
    click(x,y,MouseButton.Left, false, false, false);
  }
  @Override
  public void click(int x, int y, MouseButton button) {
    click(x,y,button, false, false, false);
  }
  @Override
  public void click(int x, int y, MouseButton button, boolean isControl, boolean isAlt, boolean isShift) {
    mouseDown(x, y, button, isControl, isAlt, isShift);
    mouseUp(x, y, button, isControl, isAlt, isShift);
  }

  @Override
  public void doubleclick(int x, int y) {
    doubleclick(x,y,MouseButton.Left, false, false, false);
  }
  @Override
  public void doubleclick(int x, int y, MouseButton button) {
    doubleclick(x,y,button, false, false, false);
  }
  @Override
  public void doubleclick(int x, int y, MouseButton button, boolean isControl, boolean isAlt, boolean isShift) {
    sendMsg(
                button.win_message_id_double,
                makeWinapiMouseEventFlags(isControl, isAlt, isShift),
                makeLParam(x, y).intValue());
  }
  
  @Override
  public void mouseOver(int x, int y) {
    sendMsg(
                Messages.MOUSEHOVER,
                0,
                makeLParam(x, y).intValue());
    sendMsg(
                Messages.MOUSEMOVE,
                0,
                makeLParam(x, y).intValue());
    sendMsg(
                Messages.NCHITTEST,
                0,
                makeLParam(x, y).intValue());
  }
  
  
  
  public static int makeWinapiMouseEventFlags(boolean isControl, boolean isAlt, boolean isShift) {
    int flags = 0;
    if (isControl) 
       flags |= 0x8;
    if (isShift) 
       flags |= 0x4;
    if (isAlt)
      throw new UnsupportedOperationException("Alt key is not supported yet. It was ignored");
    return flags;
  }
  
  @Override
  public void keyDown(int key) {
    sendMsg(256, key, 0);
  }

  @Override
  public void keyPress(int key) {
    sendMsg(258, key, 0);
  }

  @Override
  public void keyUp(int key) {
    sendMsg(257, key, 0);
  }

  @Override
  public void typeString(String text) {
   for (char key : text.toCharArray()) {
     sendMsg(258, key, 0);
   }
  }

  public int getLastError() {
    return Kernel32.INSTANCE.GetLastError();
  }
  public void clearLastError() {
    Kernel32.INSTANCE.SetLastError(0);
  }
  
  @Override
  /***
   * Maximizes the specified window.
   */
  public void maximize() {
    UserExt.ShowWindow(hwnd, 3);
  }
  /***
   * Minimizes the specified window and activates
   * the next top-level window in the Z order.
   */
  @Override
  public void minimize() {
    UserExt.ShowWindow(hwnd, 6);
  }

  @Override
  public void minimize(boolean force) {
    UserExt.ShowWindow(hwnd, force?11:6);
  }
  /**
   * See microsoft documentation: 
   * https://msdn.microsoft.com/en-us/library/windows/desktop/ms633548%28v=vs.85%29.aspx
   * @param nCmdShow 
   */
  public void ShowWindow(int nCmdShow) {
    UserExt.ShowWindow(hwnd, nCmdShow);
  }
  /** Disable window interaction**/
  @Override
  public void disable() {
    UserExt.EnableWindow(hwnd, false);
  }
  /** Enable window interaction**/
  @Override
  public void enable() {
    UserExt.EnableWindow(hwnd, true);
  }
  
  public boolean isEnabled() {
    return UserExt.IsWindowEnabled(hwnd);
  }
  /**
   *
   * @return true if the window is minimised
   */
  @Override
  public boolean isMinimized() {
   ShowWindow show = getWindowPlacement();
   return show == ShowWindow.MINIMIZE || show == ShowWindow.SHOWMINIMIZED || show == ShowWindow.FORCEMINIMIZE;
  }
  
  @Override
  public boolean isVisible() {
    return UserExt.IsWindowVisible(hwnd);
  }
  
  public boolean isValid() {
    return UserExt.IsWindow(hwnd);
  }
  
  
  private ShowWindow getWindowPlacement() {
   WinDefExt.WINDOWPLACEMENT placement = new WinDefExt.WINDOWPLACEMENT();
   UserExt.GetWindowPlacement(hwnd, placement);
   return ShowWindow.byCode(placement.showCmd);
  }
  /**
   * Activates and displays the window.
   * If the window is minimized or maximized,
   * the system restores it to its original size and position.
   * An application should specify this flag when restoring a minimized window.
   */
  @Override
  public void restore() {
    UserExt.ShowWindow(hwnd, 9);
  }
  
  @Override
  public void restoreNoActivate() {
    UserExt.ShowWindow(hwnd, ShowWindow.SHOWNOACTIVATE.code);
  }
  /**
   * Repaint window using WM_PAINT message
   */
  @Override
  public void repaint() {
    sendMsg(Messages.PAINT.code, 0, 0); 
  }
  /**
   * Hides the window (completely) and activates another window.
   */
  @Override
  public void hide() {
    UserExt.ShowWindow(hwnd, 0);
  }

  @Override
  public void focus() {
    restore();
    UserExt.SetForegroundWindow(hwnd);
  }
  @Override
  public void move(int x, int y) throws APIException {
     Rect rc = getRect();
     UserExt.MoveWindow(hwnd, x, y, (int)(rc.right - rc.left), (int)(rc.bottom - rc.top), true);
  }

  @Override
  public void resize(int w, int h) throws APIException {
     Rect rc = getRect();
     UserExt.MoveWindow(hwnd, (int)rc.left, (int)rc.top, w, h, true);
  }

  @Override
  public void moveresize(int x, int y, int w, int h) {
    UserExt.MoveWindow(hwnd, x, y, w, h, true);
  }

  /**
   * Closes the window normally (like clicking the X button). The process 
   * may not terminate after this, or ever - the application decides 
   * how is exit command handled.
   */
  @Override
  public void close() {
     WinDef.WPARAM wParam = new WinDef.WPARAM();
     WinDef.LPARAM lParam = new WinDef.LPARAM();
     
     wParam.setValue(0L);
     lParam.setValue(0L);
     UserExt.PostMessage(hwnd, 16, wParam, lParam);
  }
  

  @Override
  public Color getColor(int x, int y) {
     //BufferedImage buf = new BufferedImage(2, 2, 5);
     WinDef.HDC hdcWindow = User32Ext.INSTANCE.GetDC(hwnd);
     int color = GDIExt.GetPixel(hdcWindow, x, y);
     return ColorRef.fromNativeColor(color);
  }

  @Override
  public Color getAvgColor(int x, int y, int w, int h) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  @Override
  public BufferedImage screenshot() throws APIException
   {
   //bounds contains .width, .height, .top, .left, .bottom and .right
   Rect bounds = getRect();
   if(bounds.width == 0 || bounds.height == 0) {
     throw new APIException("One or both of the window dimensions is zero. Can't make screenshot for such dimensions.");
   }
   //Wtf is this, seriously? Random number?
   WinDef.DWORD SRCCOPY = new WinDef.DWORD(13369376L);
   //I guess here we retrieve the drawing context of the window...
   WinDef.HDC hdcWindow = User32Ext.INSTANCE.GetDC(hwnd);
   //But what is this then? Why two HDC objects?
   WinDef.HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);
   //And this is some kind of image representation?
   WinDef.HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, (int)bounds.width, (int)bounds.height);
   //This is another total mystery
   WinNT.HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
   //And what is the last parameter?!
   GDIExt.BitBlt(hdcMemDC, 0, 0, (int)bounds.width, (int)bounds.height, hdcWindow, 0, 0, SRCCOPY);
   //Select and then delete? Why did we even bother?
   GDIExt.SelectObject(hdcMemDC, hOld);
   GDIExt.DeleteDC(hdcMemDC);
   hOld = hdcMemDC = null;
   
   WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
   bmi.bmiHeader.biWidth = (int)bounds.width;
   bmi.bmiHeader.biHeight = (int)(-bounds.height);
   bmi.bmiHeader.biPlanes = 1;
   bmi.bmiHeader.biBitCount = 32;
   bmi.bmiHeader.biCompression = 0;
   //This makes sense - allocate 4 bytes for every RGBA pixel
   Memory buffer = new Memory((long)(bounds.width * bounds.height * 4));
   //Probably copying the data to memory
   GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, (int)bounds.height, buffer, bmi, 0);
   
   BufferedImage image = new BufferedImage((int)bounds.width, (int)bounds.height, 1);
   image.setRGB(0, 0, (int)bounds.width, (int)bounds.height, buffer.getIntArray(0L, (int)(bounds.width * bounds.height)), 0, (int)bounds.width);
   GDI32.INSTANCE.DeleteObject(hBitmap);
   //Release? Does this mean we were blocking window rendering until now?
   UserExt.ReleaseDC(hwnd, hdcWindow);
   return image;
  }
  
  public BufferedImage print() throws APIException
   {
   //bounds contains .width, .height, .top, .left, .bottom and .right
   Rect bounds = getRect();
   if(bounds.width == 0 || bounds.height == 0) {
     throw new APIException("One or both of the window dimensions is zero. Can't make screenshot for such dimensions.");
   }
   //Wtf is this, seriously? Random number?
   WinDef.DWORD SRCCOPY = new WinDef.DWORD(13369376L);
   //I guess here we retrieve the drawing context of the window...
   WinDef.HDC hdcWindow = User32Ext.INSTANCE.GetDC(hwnd);
   //But what is this then? Why two HDC objects?
   WinDef.HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);
   //And this is some kind of image representation?
   WinDef.HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, (int)bounds.width, (int)bounds.height);
   //This is another total mystery
   WinNT.HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
   //And what is the last parameter?!
   GDIExt.BitBlt(hdcMemDC, 0, 0, (int)bounds.width, (int)bounds.height, hdcWindow, 0, 0, SRCCOPY);
   //Select and then delete? Why did we even bother?
   GDIExt.SelectObject(hdcMemDC, hOld);
   GDIExt.DeleteDC(hdcMemDC);
   hOld = hdcMemDC = null;
   
   WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
   bmi.bmiHeader.biWidth = (int)bounds.width;
   bmi.bmiHeader.biHeight = (int)(-bounds.height);
   bmi.bmiHeader.biPlanes = 1;
   bmi.bmiHeader.biBitCount = 32;
   bmi.bmiHeader.biCompression = 0;
   //This makes sense - allocate 4 bytes for every RGBA pixel
   Memory buffer = new Memory((long)(bounds.width * bounds.height * 4));
   //Probably copying the data to memory
   GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, (int)bounds.height, buffer, bmi, 0);
   
   BufferedImage image = new BufferedImage((int)bounds.width, (int)bounds.height, 1);
   image.setRGB(0, 0, (int)bounds.width, (int)bounds.height, buffer.getIntArray(0L, (int)(bounds.width * bounds.height)), 0, (int)bounds.width);
   GDI32.INSTANCE.DeleteObject(hBitmap);
   //Release? Does this mean we were blocking window rendering until now?
   UserExt.ReleaseDC(hwnd, hdcWindow);
   return image;
  }
  public static BufferedImage screenshotAll() {
    //Wtf is this, seriously? Random number?
    WinDef.DWORD SRCCOPY = new WinDef.DWORD(13369376L);
    //I guess here we retrieve the drawing context of the window...
    WinDef.HDC hdcWindow = User32Ext.INSTANCE.GetDC(null);
    //But what is this then? Why two HDC objects?
    WinDef.HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);
    //Get screen size
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int width = (int)screenSize.getWidth();
    int height = (int)screenSize.getHeight();
    //And this is some kind of image representation?
    WinDef.HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, (int)width, (int)height);
    //This is another total mystery
    WinNT.HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
    //And what is the last parameter?!
    GDIExt.BitBlt(hdcMemDC, 0, 0, width, height, hdcWindow, 0, 0, SRCCOPY);
    //Select and then delete? Why did we even bother?
    GDIExt.SelectObject(hdcMemDC, hOld);
    GDIExt.DeleteDC(hdcMemDC);

    WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
    bmi.bmiHeader.biWidth = width;
    bmi.bmiHeader.biHeight = -height;
    bmi.bmiHeader.biPlanes = 1;
    bmi.bmiHeader.biBitCount = 32;
    bmi.bmiHeader.biCompression = 0;
    //This makes sense - allocate 4 bytes for every RGBA pixel
    Memory buffer = new Memory((long)(width * height * 4));
    //Probably copying the data to memory
    GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, height, buffer, bmi, 0);

    BufferedImage image = new BufferedImage(width, height, 1);
    image.setRGB(0, 0, width, height, buffer.getIntArray(0L, width * height), 0, width);
    GDI32.INSTANCE.DeleteObject(hBitmap);
    //Release? Does this mean we were blocking window rendering until now?
    UserExt.ReleaseDC(null, hdcWindow);
    
    return image;
  }
  
  @Override
  public BufferedImage screenshotCrop(int x, int y, int w, int h) throws APIException {
   //Wtf is this, seriously
   WinDef.DWORD SRCCOPY = new WinDef.DWORD(13369376L);

   WinDef.HDC hdcWindow = User32Ext.INSTANCE.GetDC(hwnd);
   WinDef.HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);
   //Rect bounds = getRect();
   
   /*if(x<0 || y<0 || x+w>bounds.width || y+h>bounds.height) {
     throw new IllegalArgumentException(
             "The cropped screenshot exceeds the size of the window. Real size: "+
             bounds.width+"x"+bounds.height+
             "Your size:"+(x+w)+"x"+(y+h)
     ); 
   }*/
   
   WinDef.HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, /**/ w,h /* (int)bounds.width, (int)bounds.height /**/);
   WinNT.HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
   //getGDI32();
   GDIExt.BitBlt(hdcMemDC, 0, 0, w, h, hdcWindow, x, y, SRCCOPY);
   GDIExt.SelectObject(hdcMemDC, hOld);
   GDIExt.DeleteDC(hdcMemDC);
   WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
   bmi.bmiHeader.biWidth = w;
   bmi.bmiHeader.biHeight = (-h);
   bmi.bmiHeader.biPlanes = 1;
   bmi.bmiHeader.biBitCount = 32;
   bmi.bmiHeader.biCompression = 0;
   Memory buffer = new Memory(w * h * 4);
   GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, h, buffer, bmi, 0);
   BufferedImage image = new BufferedImage(w, h, 1);
   image.setRGB(0, 0, w, h, buffer.getIntArray(0L, w * h), 0, w);
   GDI32.INSTANCE.DeleteObject(hBitmap);
   UserExt.ReleaseDC(hwnd, hdcWindow);
   return image;
  }
  
  /**
   * Creates autoclick.Rect object describing dimensions of this window.
   * @return Rect object describing window size and on-screen position
   * @throws APIException - when internal API function returns false
   */
  @Override
  public Rect getRect() throws APIException
  {
   WinDef.RECT result = new WinDef.RECT();
   
   if(!UserExt.GetWindowRect(hwnd, result)) {
     throw new APIException("API failed to retrieve valid window rectangle!"); 
   }
   return new Rect(result);
  }
  
  /** WINDOWS ONLY METHODS **/
  //Please keep all system specific methods private
  //To discourage their direct use
  private WinDef.RECT getClientRect()
  {
   WinDef.RECT result = new WinDef.RECT();
   UserExt.GetClientRect(hwnd, result);
   return result;
  }
  private int sendMsg(int msg, int wparam, int lparam) throws WindowAccessDeniedException {
    clearLastError();
    int retval = UserExt.SendMessage(hwnd, msg, new WinDef.WPARAM(wparam), new WinDef.LPARAM(lparam));
    Logger.getLogger(this.getClass().getName()).log(Level.INFO, "MESSAGE: "+msg+" "+wparam+" "+lparam);
    if(getLastError()==5) {
      throw new WindowAccessDeniedException("The message was blocked by UIPI."); 
    }
    return retval;
  }
  private void sendMsg(Messages msg, int wparam, int lparam) {
    sendMsg(msg.code, wparam, lparam);
  }
  public static User32Ext UserExt = User32Ext.INSTANCE;
  public static GDI32Ext GDIExt = GDI32Ext.INSTANCE;
  public static MSWindow windowFromName(String name, final boolean strict) {
    if(strict) {
      WinDef.HWND hwnd = UserExt.FindWindow(null, name);
      if(hwnd==null)
        return null;
      else
        return new MSWindow(hwnd);
    }
    else {
     final String name_small = name.toLowerCase();
     //I'm not entirely sure why we use array here, but
     //my guess is, that normal non-final variable would not be
     //accessible in the callback...
     final WinDef.HWND[] WindowID = { null };
     //Create ArrayList to store all windows from EnumWindows
     final ArrayList<WinDef.HWND> allWindows = new ArrayList<>();
     
     UserExt.EnumWindows(new WinUser.WNDENUMPROC()
     {
       int count = 0;
       
       @Override
       public boolean callback(WinDef.HWND handle, Pointer arg1)
       {
         /*
         //Skip non window objects, like browser tabs
         if(!UserExt.IsWindow(handle))
           return true;
         //User32 provides us with window title
         int length = UserExt.GetWindowTextLength(handle) + 1;
         char[] buf = new char[length];
         
         UserExt.GetWindowText(handle, buf, length);
         String text = String.valueOf(buf).trim();
         
         buf = null;
         
         //Now we try to match the given name in the title we obtained
         boolean result;
         //Do not trust empty strings
         if(text.isEmpty())
           result = false;
         else {
           //Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Trying window '"+text+"'.");
           result=text.toLowerCase().contains(name_small);
         }
         //If the window didn't match, return true to continue enumeration 
         if (!result) {
           return true;
         }
         //And if we gained one, put it in our array
         WindowID[0] = handle;
         //Returning false ends the enumeration
         return false;*/
         allWindows.add(handle);
         return true;
       }
     }, null);
     //Find the window in list:
     for(WinDef.HWND handle:allWindows) {
       //Skip non window objects, like browser tabs
       if(!UserExt.IsWindow(handle))
         continue;
       //User32 provides us with window title
       int length = UserExt.GetWindowTextLength(handle) + 1;
       char[] buf = new char[length];

       UserExt.GetWindowText(handle, buf, length);
       String text = String.valueOf(buf).trim();

       buf = null;

       //Now we try to match the given name in the title we obtained
       boolean result;
       //Do not trust empty strings
       if(text.isEmpty())
         result = false;
       else {
         //Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Trying window '"+text+"'.");
         result=text.toLowerCase().contains(name_small);
       }
       //If the window didn't match, return true to continue enumeration 
       if (!result) {
         continue;
       }
       Logger.getLogger(MSWindow.class.getName()).log(Level.INFO, "Found window: {0}", text);
       //And if we gained one, put it in our array
       WindowID[0] = handle;
       //Returning false ends the enumeration
       break;
     }
     
     return WindowID[0]==null?null:new MSWindow(WindowID[0]);
    }
  }
  // use max length to limit the number of windows returned
  // -1 == unlimited
  public static List<Window> windows(WindowValidator valid, int maxLength) {
   
     //I'm not entirely sure why we use array here, but
     //my guess is, that normal non-final variable would not be
     //accessible in the callback...
     final List<Window> windows = new ArrayList<>();
     UserExt.EnumWindows(new WinUser.WNDENUMPROC()
     {
       @Override
       public boolean callback(WinDef.HWND handle, Pointer arg1)
       {
         //Skip non window objects, like browser tabs
         if(!UserExt.IsWindow(handle))
           return true;
         Window w = new MSWindow(handle);
         if(valid==null || valid.isValid(w)) {
           windows.add(w);
         }
         // return if max no. of windows was found
         if(maxLength>=0 && windows.size() > maxLength) 
           return false;
         //User32 provides us with window title
         return true;
       }
     }, null);
     return windows;   
  }
  public static List<Window> windows(WindowValidator valid) {
    return windows(valid, -1);
  }
  public static Window findWindow(WindowValidator valid) {
    final List<Window> windows = windows(valid, 1);
    return windows.size()>0?windows.get(0):null;
  }
  @Deprecated
  public static MSWindow windowFromPID(final int required_pid) {
    if(true)
      throw new UnsupportedOperationException("Can't get window from ID - the function doesn't work.");
    //GetWindowThreadProcessId(WinDef.HWND hwnd, IntByReference ibr)
     final WinDef.HWND[] WindowID = { null };
     UserExt.EnumWindows(new WinUser.WNDENUMPROC()
     {
       int count = 0;
       
       @Override
       public boolean callback(WinDef.HWND handle, Pointer arg1)
       {
         //Skip non window objects, like browser tabs
         if(!UserExt.IsWindow(handle))
           return true;
         //User32 provides us with window title
        
         
         int pid = UserExt.GetWindowThreadProcessId(handle, new IntByReference(0));
         //Logger.getLogger(this.getClass().getName()).log(Level.INFO, getWindowTitle(handle)+" : "+pid);
         if(pid==required_pid) {
           //And if we gained one, put it in our array
           WindowID[0] = handle;
           //Returning false ends the enumeration
           return false;
         }
         return true;
       }
     }, null);
     
     
     return WindowID[0]==null?null:new MSWindow(WindowID[0]);
    }
  
  
    public static String getWindowTitle(WinDef.HWND handle) {
      //User32 provides us with window title
      int length = UserExt.GetWindowTextLength(handle) + 1;
      char[] buf = new char[length];

      UserExt.GetWindowText(handle, buf, length);
      return String.valueOf(buf).trim();
    }
    public static String getWindowClass(WinDef.HWND handle) {
      //User32 provides us with window title
      char[] buf = new char[255];

      UserExt.GetClassName(handle, buf, 255);
      return String.valueOf(buf).trim();
    }

  @Override
  public String getTitle() {
    return getWindowTitle(hwnd);
  }

  @Override
  public List<Window> getChildWindows() {
    //GetWindowThreadProcessId(WinDef.HWND hwnd, IntByReference ibr)
     final ArrayList<Window> list= new ArrayList<>();
     UserExt.EnumChildWindows(hwnd, new WinUser.WNDENUMPROC()
     {
       @Override
       public boolean callback(WinDef.HWND handle, Pointer arg1)
       {
         //Skip non window objects, like browser tabs
         if(!UserExt.IsWindow(handle))
           return true;
         //User32 provides us with window title
        
         
         list.add(new MSWindow(handle));
         return true;
       }
     }, null);
     
     
     return list;
  }

  @Override
  public boolean isAdmin() {
    String title = getTitle();
    String processes;
    try {
      processes = NativeProcess.readWholeOutput(Runtime.getRuntime().exec("tasklist /FO CSV /V"));
    } catch (IOException ex) {
      return false;
    }
    
    return false;
  }

  @Override
  public Process getProcess() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  
  @Override
  public String getProcessName() {
    // Refference to int that will later be filled 
    IntByReference pid = new IntByReference(0); 
    // This function gives pid number to the second parameter passed by refference
    UserExt.GetWindowThreadProcessId(hwnd, pid);
    // Now get handle to the process 
    // 0x0400 | 0x0010 stands for reading info
    // if you pass 0 you will get error 5 which stands for access denied
    int pidVal = pid.getValue();
    HANDLE process = Kernel32.INSTANCE.OpenProcess(0x0400 | 0x0010, false, pidVal);
    if(process==null)
      throw new APIException("Winapi error: "+(Kernel32.INSTANCE.GetLastError()));
    // Prepare buffer for characters, just as you would 
    // in goold 'ol C program
    char[] path = new char[150];
    DWORD buffSize = new DWORD(path.length);
    // The W at the end of the function name stands for WIDE - 2byte chars
    Psapi.INSTANCE.GetModuleFileNameExW(process, null, path, buffSize);
    // convert buffer to java string
    return new String(path).split("\0")[0];
  }

  @Override
  public boolean isForeground() {
    final WinDef.HWND window = UserExt.GetForegroundWindow();
    return window==this.hwnd || (window!=null && window.equals(this.hwnd));
  }
  
}
