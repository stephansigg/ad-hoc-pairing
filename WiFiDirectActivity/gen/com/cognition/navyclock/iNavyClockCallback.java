/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\SOFTWARE\\adt-bundle-windows-x86-20130729\\android_workspace\\WiFiDirectActivity\\src\\com\\cognition\\navyclock\\iNavyClockCallback.aidl
 */
package com.cognition.navyclock;
public interface iNavyClockCallback extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.cognition.navyclock.iNavyClockCallback
{
private static final java.lang.String DESCRIPTOR = "com.cognition.navyclock.iNavyClockCallback";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.cognition.navyclock.iNavyClockCallback interface,
 * generating a proxy if needed.
 */
public static com.cognition.navyclock.iNavyClockCallback asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.cognition.navyclock.iNavyClockCallback))) {
return ((com.cognition.navyclock.iNavyClockCallback)iin);
}
return new com.cognition.navyclock.iNavyClockCallback.Stub.Proxy(obj);
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
case TRANSACTION_onTimeUpdate:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
long _arg1;
_arg1 = data.readLong();
this.onTimeUpdate(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_onClockStateChange:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
this.onClockStateChange(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.cognition.navyclock.iNavyClockCallback
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
	 * Called frequently to notify the callback of the current atomic time.
	 *
	 * atomicTime is the actual atomic time.
	 * localTime is the time on the phone that the atomicTime was measured.
	 *
	 * The difference between atomicTime and localTime can be interpreted as the offset
	 * or skew of the local clock from the atomic clock.
	 */
@Override public void onTimeUpdate(long atomicTime, long localTime) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(atomicTime);
_data.writeLong(localTime);
mRemote.transact(Stub.TRANSACTION_onTimeUpdate, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/*
	  * Notifies the callback of the current atomic clock state.  Will be one of the 
	  * states defined in the class AtomicClockState
	  *
	  * newState - the identifier of the new state
	  * server   - the name of the server that the service is currently talking to.
	  * location - the common name/description of the server
	  */
@Override public void onClockStateChange(int newState, java.lang.String server, java.lang.String location) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(newState);
_data.writeString(server);
_data.writeString(location);
mRemote.transact(Stub.TRANSACTION_onClockStateChange, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onTimeUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onClockStateChange = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
/*
	 * Called frequently to notify the callback of the current atomic time.
	 *
	 * atomicTime is the actual atomic time.
	 * localTime is the time on the phone that the atomicTime was measured.
	 *
	 * The difference between atomicTime and localTime can be interpreted as the offset
	 * or skew of the local clock from the atomic clock.
	 */
public void onTimeUpdate(long atomicTime, long localTime) throws android.os.RemoteException;
/*
	  * Notifies the callback of the current atomic clock state.  Will be one of the 
	  * states defined in the class AtomicClockState
	  *
	  * newState - the identifier of the new state
	  * server   - the name of the server that the service is currently talking to.
	  * location - the common name/description of the server
	  */
public void onClockStateChange(int newState, java.lang.String server, java.lang.String location) throws android.os.RemoteException;
}
