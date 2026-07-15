package com.personalchat.app.data.database;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.personalchat.app.data.model.Conversation;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings({"unchecked", "deprecation"})
public final class ConversationDao_Impl implements ConversationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Conversation> __insertionAdapterOfConversation;

  private final EntityDeletionOrUpdateAdapter<Conversation> __updateAdapterOfConversation;

  private final SharedSQLiteStatement __preparedStmtOfUpdateConnectionState;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLastMessage;

  private final SharedSQLiteStatement __preparedStmtOfDeleteConversation;

  public ConversationDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfConversation = new EntityInsertionAdapter<Conversation>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `conversations` (`id`,`peerName`,`peerPhone`,`peerPhoneHash`,`peerPublicKey`,`lastMessage`,`lastMessageTimestamp`,`connectionState`,`unreadCount`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Conversation value) {
        if (value.getId() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getId());
        }
        if (value.getPeerName() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getPeerName());
        }
        if (value.getPeerPhone() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getPeerPhone());
        }
        if (value.getPeerPhoneHash() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getPeerPhoneHash());
        }
        if (value.getPeerPublicKey() == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.getPeerPublicKey());
        }
        if (value.getLastMessage() == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.getLastMessage());
        }
        stmt.bindLong(7, value.getLastMessageTimestamp());
        if (value.getConnectionState() == null) {
          stmt.bindNull(8);
        } else {
          stmt.bindString(8, value.getConnectionState());
        }
        stmt.bindLong(9, value.getUnreadCount());
      }
    };
    this.__updateAdapterOfConversation = new EntityDeletionOrUpdateAdapter<Conversation>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `conversations` SET `id` = ?,`peerName` = ?,`peerPhone` = ?,`peerPhoneHash` = ?,`peerPublicKey` = ?,`lastMessage` = ?,`lastMessageTimestamp` = ?,`connectionState` = ?,`unreadCount` = ? WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Conversation value) {
        if (value.getId() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getId());
        }
        if (value.getPeerName() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getPeerName());
        }
        if (value.getPeerPhone() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getPeerPhone());
        }
        if (value.getPeerPhoneHash() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getPeerPhoneHash());
        }
        if (value.getPeerPublicKey() == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.getPeerPublicKey());
        }
        if (value.getLastMessage() == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.getLastMessage());
        }
        stmt.bindLong(7, value.getLastMessageTimestamp());
        if (value.getConnectionState() == null) {
          stmt.bindNull(8);
        } else {
          stmt.bindString(8, value.getConnectionState());
        }
        stmt.bindLong(9, value.getUnreadCount());
        if (value.getId() == null) {
          stmt.bindNull(10);
        } else {
          stmt.bindString(10, value.getId());
        }
      }
    };
    this.__preparedStmtOfUpdateConnectionState = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE conversations SET connectionState = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateLastMessage = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE conversations SET lastMessage = ?, lastMessageTimestamp = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteConversation = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM conversations WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public void insert(final Conversation conversation) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfConversation.insert(conversation);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final Conversation conversation) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfConversation.handle(conversation);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateConnectionState(final String id, final String state) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateConnectionState.acquire();
    int _argIndex = 1;
    if (state == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, state);
    }
    _argIndex = 2;
    if (id == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, id);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfUpdateConnectionState.release(_stmt);
    }
  }

  @Override
  public void updateLastMessage(final String id, final String lastMsg, final long timestamp) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLastMessage.acquire();
    int _argIndex = 1;
    if (lastMsg == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, lastMsg);
    }
    _argIndex = 2;
    _stmt.bindLong(_argIndex, timestamp);
    _argIndex = 3;
    if (id == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, id);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfUpdateLastMessage.release(_stmt);
    }
  }

  @Override
  public void deleteConversation(final String id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteConversation.acquire();
    int _argIndex = 1;
    if (id == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, id);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteConversation.release(_stmt);
    }
  }

  @Override
  public LiveData<List<Conversation>> getAllConversations() {
    final String _sql = "SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[]{"conversations"}, false, new Callable<List<Conversation>>() {
      @Override
      public List<Conversation> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPeerName = CursorUtil.getColumnIndexOrThrow(_cursor, "peerName");
          final int _cursorIndexOfPeerPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "peerPhone");
          final int _cursorIndexOfPeerPhoneHash = CursorUtil.getColumnIndexOrThrow(_cursor, "peerPhoneHash");
          final int _cursorIndexOfPeerPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "peerPublicKey");
          final int _cursorIndexOfLastMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessage");
          final int _cursorIndexOfLastMessageTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTimestamp");
          final int _cursorIndexOfConnectionState = CursorUtil.getColumnIndexOrThrow(_cursor, "connectionState");
          final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unreadCount");
          final List<Conversation> _result = new ArrayList<Conversation>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final Conversation _item;
            _item = new Conversation();
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            _item.setId(_tmpId);
            final String _tmpPeerName;
            if (_cursor.isNull(_cursorIndexOfPeerName)) {
              _tmpPeerName = null;
            } else {
              _tmpPeerName = _cursor.getString(_cursorIndexOfPeerName);
            }
            _item.setPeerName(_tmpPeerName);
            final String _tmpPeerPhone;
            if (_cursor.isNull(_cursorIndexOfPeerPhone)) {
              _tmpPeerPhone = null;
            } else {
              _tmpPeerPhone = _cursor.getString(_cursorIndexOfPeerPhone);
            }
            _item.setPeerPhone(_tmpPeerPhone);
            final String _tmpPeerPhoneHash;
            if (_cursor.isNull(_cursorIndexOfPeerPhoneHash)) {
              _tmpPeerPhoneHash = null;
            } else {
              _tmpPeerPhoneHash = _cursor.getString(_cursorIndexOfPeerPhoneHash);
            }
            _item.setPeerPhoneHash(_tmpPeerPhoneHash);
            final String _tmpPeerPublicKey;
            if (_cursor.isNull(_cursorIndexOfPeerPublicKey)) {
              _tmpPeerPublicKey = null;
            } else {
              _tmpPeerPublicKey = _cursor.getString(_cursorIndexOfPeerPublicKey);
            }
            _item.setPeerPublicKey(_tmpPeerPublicKey);
            final String _tmpLastMessage;
            if (_cursor.isNull(_cursorIndexOfLastMessage)) {
              _tmpLastMessage = null;
            } else {
              _tmpLastMessage = _cursor.getString(_cursorIndexOfLastMessage);
            }
            _item.setLastMessage(_tmpLastMessage);
            final long _tmpLastMessageTimestamp;
            _tmpLastMessageTimestamp = _cursor.getLong(_cursorIndexOfLastMessageTimestamp);
            _item.setLastMessageTimestamp(_tmpLastMessageTimestamp);
            final String _tmpConnectionState;
            if (_cursor.isNull(_cursorIndexOfConnectionState)) {
              _tmpConnectionState = null;
            } else {
              _tmpConnectionState = _cursor.getString(_cursorIndexOfConnectionState);
            }
            _item.setConnectionState(_tmpConnectionState);
            final int _tmpUnreadCount;
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
            _item.setUnreadCount(_tmpUnreadCount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public List<Conversation> getAllConversationsSync() {
    final String _sql = "SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfPeerName = CursorUtil.getColumnIndexOrThrow(_cursor, "peerName");
      final int _cursorIndexOfPeerPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "peerPhone");
      final int _cursorIndexOfPeerPhoneHash = CursorUtil.getColumnIndexOrThrow(_cursor, "peerPhoneHash");
      final int _cursorIndexOfPeerPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "peerPublicKey");
      final int _cursorIndexOfLastMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessage");
      final int _cursorIndexOfLastMessageTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTimestamp");
      final int _cursorIndexOfConnectionState = CursorUtil.getColumnIndexOrThrow(_cursor, "connectionState");
      final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unreadCount");
      final List<Conversation> _result = new ArrayList<Conversation>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Conversation _item;
        _item = new Conversation();
        final String _tmpId;
        if (_cursor.isNull(_cursorIndexOfId)) {
          _tmpId = null;
        } else {
          _tmpId = _cursor.getString(_cursorIndexOfId);
        }
        _item.setId(_tmpId);
        final String _tmpPeerName;
        if (_cursor.isNull(_cursorIndexOfPeerName)) {
          _tmpPeerName = null;
        } else {
          _tmpPeerName = _cursor.getString(_cursorIndexOfPeerName);
        }
        _item.setPeerName(_tmpPeerName);
        final String _tmpPeerPhone;
        if (_cursor.isNull(_cursorIndexOfPeerPhone)) {
          _tmpPeerPhone = null;
        } else {
          _tmpPeerPhone = _cursor.getString(_cursorIndexOfPeerPhone);
        }
        _item.setPeerPhone(_tmpPeerPhone);
        final String _tmpPeerPhoneHash;
        if (_cursor.isNull(_cursorIndexOfPeerPhoneHash)) {
          _tmpPeerPhoneHash = null;
        } else {
          _tmpPeerPhoneHash = _cursor.getString(_cursorIndexOfPeerPhoneHash);
        }
        _item.setPeerPhoneHash(_tmpPeerPhoneHash);
        final String _tmpPeerPublicKey;
        if (_cursor.isNull(_cursorIndexOfPeerPublicKey)) {
          _tmpPeerPublicKey = null;
        } else {
          _tmpPeerPublicKey = _cursor.getString(_cursorIndexOfPeerPublicKey);
        }
        _item.setPeerPublicKey(_tmpPeerPublicKey);
        final String _tmpLastMessage;
        if (_cursor.isNull(_cursorIndexOfLastMessage)) {
          _tmpLastMessage = null;
        } else {
          _tmpLastMessage = _cursor.getString(_cursorIndexOfLastMessage);
        }
        _item.setLastMessage(_tmpLastMessage);
        final long _tmpLastMessageTimestamp;
        _tmpLastMessageTimestamp = _cursor.getLong(_cursorIndexOfLastMessageTimestamp);
        _item.setLastMessageTimestamp(_tmpLastMessageTimestamp);
        final String _tmpConnectionState;
        if (_cursor.isNull(_cursorIndexOfConnectionState)) {
          _tmpConnectionState = null;
        } else {
          _tmpConnectionState = _cursor.getString(_cursorIndexOfConnectionState);
        }
        _item.setConnectionState(_tmpConnectionState);
        final int _tmpUnreadCount;
        _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
        _item.setUnreadCount(_tmpUnreadCount);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Conversation getConversationById(final String id) {
    final String _sql = "SELECT * FROM conversations WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfPeerName = CursorUtil.getColumnIndexOrThrow(_cursor, "peerName");
      final int _cursorIndexOfPeerPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "peerPhone");
      final int _cursorIndexOfPeerPhoneHash = CursorUtil.getColumnIndexOrThrow(_cursor, "peerPhoneHash");
      final int _cursorIndexOfPeerPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "peerPublicKey");
      final int _cursorIndexOfLastMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessage");
      final int _cursorIndexOfLastMessageTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTimestamp");
      final int _cursorIndexOfConnectionState = CursorUtil.getColumnIndexOrThrow(_cursor, "connectionState");
      final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unreadCount");
      final Conversation _result;
      if(_cursor.moveToFirst()) {
        _result = new Conversation();
        final String _tmpId;
        if (_cursor.isNull(_cursorIndexOfId)) {
          _tmpId = null;
        } else {
          _tmpId = _cursor.getString(_cursorIndexOfId);
        }
        _result.setId(_tmpId);
        final String _tmpPeerName;
        if (_cursor.isNull(_cursorIndexOfPeerName)) {
          _tmpPeerName = null;
        } else {
          _tmpPeerName = _cursor.getString(_cursorIndexOfPeerName);
        }
        _result.setPeerName(_tmpPeerName);
        final String _tmpPeerPhone;
        if (_cursor.isNull(_cursorIndexOfPeerPhone)) {
          _tmpPeerPhone = null;
        } else {
          _tmpPeerPhone = _cursor.getString(_cursorIndexOfPeerPhone);
        }
        _result.setPeerPhone(_tmpPeerPhone);
        final String _tmpPeerPhoneHash;
        if (_cursor.isNull(_cursorIndexOfPeerPhoneHash)) {
          _tmpPeerPhoneHash = null;
        } else {
          _tmpPeerPhoneHash = _cursor.getString(_cursorIndexOfPeerPhoneHash);
        }
        _result.setPeerPhoneHash(_tmpPeerPhoneHash);
        final String _tmpPeerPublicKey;
        if (_cursor.isNull(_cursorIndexOfPeerPublicKey)) {
          _tmpPeerPublicKey = null;
        } else {
          _tmpPeerPublicKey = _cursor.getString(_cursorIndexOfPeerPublicKey);
        }
        _result.setPeerPublicKey(_tmpPeerPublicKey);
        final String _tmpLastMessage;
        if (_cursor.isNull(_cursorIndexOfLastMessage)) {
          _tmpLastMessage = null;
        } else {
          _tmpLastMessage = _cursor.getString(_cursorIndexOfLastMessage);
        }
        _result.setLastMessage(_tmpLastMessage);
        final long _tmpLastMessageTimestamp;
        _tmpLastMessageTimestamp = _cursor.getLong(_cursorIndexOfLastMessageTimestamp);
        _result.setLastMessageTimestamp(_tmpLastMessageTimestamp);
        final String _tmpConnectionState;
        if (_cursor.isNull(_cursorIndexOfConnectionState)) {
          _tmpConnectionState = null;
        } else {
          _tmpConnectionState = _cursor.getString(_cursorIndexOfConnectionState);
        }
        _result.setConnectionState(_tmpConnectionState);
        final int _tmpUnreadCount;
        _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
        _result.setUnreadCount(_tmpUnreadCount);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
