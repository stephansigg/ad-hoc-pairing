/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\SOFTWARE\\adt-bundle-windows-x86-20130729\\android_workspace\\WiFiDirectActivity\\src\\com\\cognition\\navyclock\\iNavyClock.aidl
 */
package com.cognition.navyclock;
public interface iNavyClock extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.cognition.navyclock.iNavyClock
{
private static final java.lang.String DESCRIPTOR = "com.cognition.navyclock.iNavyClock";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.cognition.navyclock.iNavyClock interface,
 * generating a proxy if needed.
 */
public static com.cognition.navyclock.iNavyClock asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.cognition.navyclock.iNavyClock))) {
return ((com.cognition.navyclock.iNavyClock)iin);
}
return new com.cognition.navyclock.iNavyClock.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_registerTimeListener:
{
data.enforceInterface(DESCRIPTOR);
com.cognition.navyclock.iNavyClockCallback _arg0;
_arg0 = com.cognition.navyclock.iNavyClockCallback.Stub.asInterface(data.readStrongBinder());
this.registerTimeListener(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_unregisterTimeListener:
{
data.enforceInterface(DESCRIPTOR);
com.cognition.navyclock.iNavyClockCallback _arg0;
_arg0 = com.cognition.navyclock.iNavyClockCallback.Stub.asInterface(data.readStrongBinder());
this.unregisterTimeListener(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.cognition.navyclock.iNavyClock
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
/*
	 * Requests current time to be sent to the callback on the specified interval.
	 * Intervals are specified in milliseconds.
	 */
@Override public void registerTimeListener(com.cognition.navyclock.iNavyClockCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_registerTimeListener, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/*
     * Remove a previously registered callback interface.
     */
@Override public void unregisterTimeListener(com.cognition.navyclock.iNavyClockCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_unregisterTimeListener, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_registerTimeListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_unregisterTimeListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
/*
	 * Requests current time to be sent to the callback on the specified interval.
	 * Intervals are specified in milliseconds.
	 */
public void registerTimeListener(com.cognition.navyclock.iNavyClockCallback callback) throws android.os.RemoteException;
/*
     * Remove a previously registered callback interface.
     */
public void unregisterTimeListener(com.cognition.navyclock.iNavyClockCallback callback) throws android.os.RemoteException;
}
