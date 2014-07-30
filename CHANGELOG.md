# Changelog

0.5.0
* Updated art work

0.4.3
* Removed async JSON Parser

0.4.2
* Fixes an Exception for not complete contacts

0.4.1
* Changed URL from https to http (fixes the lacking peer certificate error)

0.4
* Removed the trace handler
* Less verbose with the notification manager
* Improved error messages
* Memory handling improvements
* Sync company and title
* Sync the date of birth

0.3.2
* Bugfixes

0.3.1
* Bugfix for concurrently running sync adapters
* Bugfix for a possible NullPointer

0.3
* Removed size limit for fetched number of contacts
* Fixed a problem with importing a large number of contacts
* Improved the parsing of address data. This fixes the behavior that not all contact data was visible.

0.2
* Added syncronization of addresses
* Enable the sync automatically for newly added accounts

0.1
* Initial release (Supports syncing name, e-mails, URLs, phone numbers and picture)
