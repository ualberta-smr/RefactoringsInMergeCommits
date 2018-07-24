from peewee import *

database = MySQLDatabase('refactoring_analysis', **{'use_unicode': True, 'passwd': 'root', 'charset': 'utf8', 'user': 'root'})


class UnknownField(object):
    def __init__(self, *_, **__): pass


class BaseModel(Model):
    class Meta:
        database = database


class Project(BaseModel):
    name = CharField(null=True)
    url = CharField(null=True)

    class Meta:
        table_name = 'project'


class MergeCommit(BaseModel):
    commit_hash = CharField(primary_key=True)
    common_ancestor_hash = CharField(null=True)
    has_conflict = IntegerField(null=True)
    project = ForeignKeyField(column_name='project', field='id', model=Project)

    class Meta:
        table_name = 'merge_commit'


class ConflictingFile(BaseModel):
    merge_commit = ForeignKeyField(column_name='merge_commit', field='commit_hash', model=MergeCommit)
    package = CharField(null=True)
    path = CharField()
    type = CharField()

    class Meta:
        table_name = 'conflicting_file'


class MergeParent(BaseModel):
    commit_hash = CharField()
    merge_commit = ForeignKeyField(column_name='merge_commit', field='commit_hash', model=MergeCommit)

    class Meta:
        table_name = 'merge_parent'


class RefactoringCommit(BaseModel):
    commit_hash = CharField(null=True)
    destination_class = CharField(null=True)
    merge_parent = ForeignKeyField(column_name='merge_parent', field='id', model=MergeParent, null=True)
    refactoring_detail = CharField(null=True)
    refactoring_type = CharField(null=True)
    source_class = CharField(null=True)

    class Meta:
        table_name = 'refactoring_commit'

    class Meta:
        table_name = 'refactoring_commit'
