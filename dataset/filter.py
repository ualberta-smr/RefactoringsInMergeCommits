import csv
with open('dataset.csv') as csvfile:
    reader = csv.DictReader(csvfile)
    for row in reader:
        if row['language'].lower() == 'java' and row['stars'] != 'None' and int(row['stars']) >= 100 and (row['randomforest_org'] == '1' or row['randomforest_utl'] == '1'):
            print('https://github.com/' + row['repository'])
