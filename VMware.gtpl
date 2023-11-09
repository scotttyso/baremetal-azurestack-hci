# Accept the VMware End User License Agreement
vmaccepteula
# Set the root password for the DCUI and Tech Support Mode
rootpw --iscrypted {{ .answers.RootPassword }}
#for Local boot
DISKIDPLACEHOLDER
# Set the network to DHCP on the first network adapater
{{if (eq .answers.IpVersion "V4")}}
    {{if (eq .answers.IpConfigType "DHCP")}}network --bootproto=dhcp {{ if .answers.NetworkDevice }} --device={{ .answers.NetworkDevice }} {{ end }} {{ if .answers.Vlanid }} --vlanid={{ .answers.Vlanid }} {{ end }} {{end}}
    {{if (eq .answers.IpConfigType "static")}}network --bootproto=static {{ if .answers.NetworkDevice }} --device={{ .answers.NetworkDevice }} {{ end }} {{ if .answers.Vlanid }} --vlanid={{ .answers.Vlanid }} {{ end }} --ip={{ .answers.IpV4Config.IpAddress }} --netmask={{ .answers.IpV4Config.Netmask }} --gateway={{ .answers.IpV4Config.Gateway }} --hostname={{ .answers.Hostname }} --nameserver={{ .answers.NameServer }} {{end}}
{{end}}
%pre --interpreter=busybox
hwclock -d %LIVE_VAR_DATE_1% -t %LIVE_VAR_TIME_UTC_1%
date -s %LIVE_VAR_DATE_TIME_UTC_1%
cd /tmp
%firstboot --interpreter=busybox
cd /tmp
esxcfg-vswitch -A 'VM Network' vSwitch0
###############################
# enable & start remote ESXi Shell (SSH)
###############################
vim-cmd hostsvc/enable_ssh
vim-cmd hostsvc/start_ssh
 
###############################
# enable & start ESXi Shell (TSM)
###############################
vim-cmd hostsvc/enable_esx_shell
vim-cmd hostsvc/start_esx_shell
{{with .answers.Hostname}}esxcli system hostname set --host={{.}}{{end}}
{{if (eq .answers.IpVersion "V6")}}
{{if (eq .answers.IpConfigType "static")}}
esxcli network ip set --ipv6-enabled=true
esxcli network ip interface ipv6 set -i vmk0 --enable-dhcpv6=false
esxcli network ip interface ipv6 set -i vmk0 --enable-router-adv=false
esxcli network ip interface ipv6 address add --interface-name=vmk0 --ipv6={{ .answers.IpV6Config.IpAddress }}/{{ .answers.IpV6Config.Prefix }}
esxcli network ip interface ipv6 set -i vmk0 -g {{ .answers.IpV6Config.Gateway }}
esxcli network ip interface ipv6 set -i vmk0 --enable-router-adv=true
esxcli network ip dns server add -s {{ .answers.NameServer}}
{{end}}
{{if (eq .answers.IpConfigType "DHCP")}}
esxcli network ip set --ipv6-enabled=false
esxcli network ip interface ipv6 set -i vmk0 --enable-dhcpv6=true
{{end}}
{{end}}
# Script executed for configuration to be retained after secure boot is enabled
{{if (eq .answers.SecureBoot "enabled")}}
/sbin/backup.sh 0
sleep 15
{{end}}
%post --interpreter=busybox --ignorefailure=true
ESXI_INSTALL_LOG=/var/log/esxi_install.log
echo \"OS INSTALL COMPLETED\" >> /var/log/Xinstall.log
/opt/ucs_tool_esxi/ucs_ipmitool write_file /var/log/Xinstall.log osProgress.log
cd /tmp
localcli network firewall set --default-action true
localcli network firewall set --enabled false
localcli network firewall set --default-action false
localcli network firewall set --enabled true
# Let us poweroff/shutdown our selves.
reboot
