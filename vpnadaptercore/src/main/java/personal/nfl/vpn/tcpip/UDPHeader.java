package personal.nfl.vpn.tcpip;

import personal.nfl.vpn.utils.CommonMethods;

/**
 * UDP 报文解析
 */
public class UDPHeader {
	/**
	 * UDP 数据报格式
	 * 头部长度：8 字节
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜  １６位源端口号         ｜   １６位目的端口号            ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜  １６位ＵＤＰ长度       ｜   １６位ＵＤＰ检验和          ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                  数据（如果有）                          ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 **/

	/**
	 * 下面 4 个偏移量是 UDP 报文中各属性的字节位置
	 */
	static final short offset_src_port = 0; // 源端口
	static final short offset_dest_port = 2; // 目的端口
	static final short offset_tlen = 4; // 数据报长度
	static final short offset_crc = 6; // 校验和

	public byte[] mData;
	public int mOffset;

	/**
	 * @param data 报文数据，有可能是 ip 报文 或 UDP 报文 ，所以解析报文信息时需要根据 offset 进行操作
	 * @param offset UDP 信息相对于报文数据 data 的偏移量，IP 报文时为 20 ，UDP 时为 0 。
	 */
	public UDPHeader(byte[] data, int offset) {
		mData = data;
		mOffset = offset;
	}

	/**
	 * 获得源端口号
	 * @return
	 */
	public short getSourcePort() {
		return CommonMethods.readShort(mData, mOffset + offset_src_port);
	}

	/**
	 * 修改报文中的 源端口号
	 * @param sourcePort 端口号
	 */
	public void setSourcePort(short sourcePort) {
		CommonMethods.writeShort(mData, mOffset + offset_src_port, sourcePort);
	}

	public short getDestinationPort() {
		return CommonMethods.readShort(mData, mOffset + offset_dest_port);
	}

	public void setDestinationPort(short value) {
		CommonMethods.writeShort(mData, mOffset + offset_dest_port, value);
	}

	public int getTotalLength() {
		return CommonMethods.readShort(mData, mOffset + offset_tlen) & 0xFFFF;
	}

	public void setTotalLength(int value) {
		CommonMethods.writeShort(mData, mOffset + offset_tlen, (short) value);
	}

	public short getCrc() {
		return CommonMethods.readShort(mData, mOffset + offset_crc);
	}

	public void setCrc(short value) {
		CommonMethods.writeShort(mData, mOffset + offset_crc, value);
	}

	@Override
	public String toString() {
		return String.format("%d->%d", getSourcePort() & 0xFFFF, getDestinationPort() & 0xFFFF);
	}
}
