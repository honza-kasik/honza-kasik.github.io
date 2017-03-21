---
title: vpsFree and Nextcloud
excerpt: For a long time I used DropBox to keep my files synced between devices. Since the amount of available space on my free account was getting thinner each day, I finnaly got myself a membership in vpsFree.cz association...
tags:
- android
- sys admin
---

For a long time I used DropBox to keep my files synced between devices. Since the amount of available space on my free account was getting thinner each day, I finally got myself a membership in vpsFree.cz association which provides VPS to its members. For 900 CZK / 36 EUR per three months you get 8 cores CPU, 4096 megabytes of RAM, 120 gigabytes of hard drive space, one IPv4 address and 32 IPv6 addresses plus additional 250 gigabytes space on NAS drive.

Since I wanted both to solve my file-sync problem and to refresh my knowledge about server configuration, I hopped into it and started. From several operating systems I chose CentOS 7 for my VPS. Initial configuration of SSH and other services was easy, but then the installation of Nextcloud came.

Luckily I found [some guide on marksei.com](http://www.marksei.com/install-nextcloud-11-centos-7/). Although this guide provides some information it is not complete and some commands are not explained as they should be, so I try to recreate it better.

## Step 1 - Software installation

Nextcloud requires PHP 5.6 and higher, since there is only PHP 5.4 in CentOS repository you have to get newer version in your system somehow. The easiest way is to use [Webtatic yum repository](https://webtatic.com/projects/yum-repository/) which provides up-to-date PHP packages. You can simply install it by running:

```
rpm -Uvh https://mirror.webtatic.com/yum/el7/webtatic-release.rpm
```

There are surely other ways how to get recent PHP to your system. Better way would be to use [Software Collections](https://www.softwarecollections.org/en/) but I didn't have time to find out how to configure PHP from SCL to run with stock httpd. Using Webtatic makes you dependent on repository owner. When he closes up his business, you have to package security patches yourself.

You will also need [EPEL repository](https://fedoraproject.org/wiki/EPEL) enabled. Install it by running:

```
rpm -Uvh https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
```

If you used Webtatic repo, you can now install prerequisities for Nextcloud, including httpd, by running:

```
yum install httpd php70w php70w-dom php70w-mbstring php70w-gd php70w-pdo php70w-json php70w-xml php70w-zip php70w-curl php70w-mcrypt php70w-pear setroubleshoot-server
```

## Step 2 - Database setup

Original article offers three options for databases. I rejected SQLite on first glance since I don't want to solve performance issues - from remaining options I selected MariaDB since, in my opinion, it is more widely used than PostgreSQL and, in case of any problems I will more likely find help.

You just need to install MariaDB server and PHP hooks by running

```
yum install mariadb-server php70w-mysql
```

The preffered way how to create initial configuration is to run `myql_secure_installation` script. In my case I have to start mariadb service first by running

```
systemctl start mariadb
```

and then I was able to finish configuration using mentioned script. Also don't forget enable starting mariadb service at boot:

```
systemctl enable mariadb
```

Last but one step in database setup is configuring database itself. In this step database `nextcloud` is created along with user `nc_user` who has all privileges over database `nextcloud`. When you are done, press CTRL+D to exit.

```
CREATE DATABASE nextcloud;
CREATE USER 'nc_user'@'localhost' IDENTIFIED BY 'YOUR_PASSWORD_HERE';
NT ALL PRIVILEGES ON nextcloud.* TO 'nc_user'@'localhost';
FLUSH PRIVILEGES;
```

Finally, restart `httpd`:

```
systemctl restart httpd
```

## Step 3 - Installing Nextcloud

In this case, installation is done in `/var/www`, but you can choose other path at your discretion.

```
cd /var/www
curl -OJ https://download.nextcloud.com/server/releases/latest-11.zip
tar -xvfj latest-11.zip
# set ownership of nextcloud folder to user apache
chown -R apache:apache nextcloud
# remove installation file
rm latest-11.zip
```

In original guide there is mentioned apache configuration which causes nextcloud to be running as root server. Since I wanted to run multiple servers on my machine and to use Let's encrypt for providing https I decided to use VirtualHost configuration:

```
<VirtualHost *:80>
   ServerName fooserver.com
   DocumentRoot /var/www/nextcloud

   <Directory /var/www/nextcloud>
      Options +FollowSymlinks
      AllowOverride All

      <IfModule mod_dav.c>
         Dav off
      </IfModule>

      SetEnv HOME /var/www/nextcloud
      SetEnv HTTP_HOME /var/www/nextcloud

   </Directory>
</VirtualHost>
```

Just add this file to `/etc/httpd/conf.d`, restart apache `systemctl restart httpd` and you are set.

You will now find running Nextcloud at the root path of your server. You can now also point domain to your server's IP address using A/AAAA DNS record. Alternatively you can also set sub-domain to ServerName and then add corresponding records to DNS configuration.

## Step 4 - It's alive!

Since SELinux is not enabled in VPS I didn't have to jump around SELinux context. The only thing which left is the installation itself. Just navigate to Nextcloud and finish installation using web installer.

Just don't forget to enable start httpd at startup:

```
systemctl enable httpd
```

## Step 5 - Let's encrypt

For more private connection to the server just install certbot and run:

```
certbot --apache
```

and follow directions in interactive guide if you configured ServerName and corresponding DNS records for your domain previously. Voila - you have your own file sync service secured with your own Let's encrypt certificate. Certbot automatically creates SSL enabled configuration for Apache alongside with redirect to https version of site.
