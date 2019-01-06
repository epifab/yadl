package io.epifab.yadl.domain

import shapeless.{HNil, ::}

sealed trait Statement

sealed trait SideEffect

sealed trait Select[V] extends Statement {
  def dataSource: DataSource
  def reader: Reader[V]
  def joins: Seq[Join]
  def filter: Filter
  def sort: Seq[Sort]
  def limit: Option[Limit]

  def fields: Seq[Field[_]] = reader.fields

  def take[V2](r1: Reader[V2]): Select[V2]

  def take[S2, T2](subQuery: SubQuery[S2, T2]): Select[S2]

  def leftJoin[T <: DataSource](relation: Relation[T]): Select[V]

  def innerJoin[T <: DataSource](relation: Relation[T]): Select[V]

  def crossJoin(dataSource: DataSource): Select[V]

  def where(filter: Filter): Select[V]

  def sortBy(sort: Sort*): Select[V]

  def inRange(start: Int, stop: Int): Select[V]
}

object Select {
  case class SelectImpl[V](
    dataSource: DataSource,
    reader: Reader[V],
    joins: Seq[Join] = Seq.empty,
    filter: Filter = Filter.Empty,
    sort: Seq[Sort] = Seq.empty,
    limit: Option[Limit] = None
  ) extends Select[V] {

    def take[V2](r1: Reader[V2]): Select[V2] =
      copy(reader = r1)

    def take[S2, T2](subQuery: SubQuery[S2, T2]): Select[S2] =
      copy(reader = subQuery.*)

    def leftJoin[T <: DataSource](relation: Relation[T]): Select[V] =
      copy(joins = joins :+ LeftJoin(relation, relation.clause))

    def innerJoin[T <: DataSource](relation: Relation[T]): Select[V] =
      copy(joins = joins :+ InnerJoin(relation, relation.clause))

    def crossJoin(dataSource: DataSource): Select[V] =
      copy(joins = joins :+ CrossJoin(dataSource))

    def where(filter: Filter): Select[V] =
      copy(filter = this.filter and filter)

    def sortBy(sort: Sort*): Select[V] =
      copy(sort = this.sort ++ sort)

    def inRange(start: Int, stop: Int): Select[V] =
      copy(limit = Some(Limit(start, stop)))
  }

  def from(dataSource: DataSource): Select[HNil] = SelectImpl(dataSource, Reader(HNil))
}

sealed trait Insert[T] extends Statement with SideEffect {
  def table: Table[T]
  def columnValues: Seq[ColumnValue[_]]

  def set(t: T): Insert[T]
  def set(columnValues: ColumnValue[_]*): Insert[T]
}

object Insert {
  protected final case class InsertImpl[T](table: Table[T], columnValues: Seq[ColumnValue[_]] = Seq.empty) extends Insert[T] {
    def set(t: T): Insert[T] =
      copy(columnValues = table.*.values(t))

    def set(columnValues: ColumnValue[_]*): Insert[T] =
      copy(columnValues = columnValues)
  }

  def into[T](table: Table[T]) = InsertImpl(table)
}

sealed trait Update[T] extends Statement with SideEffect {
  def table: Table[T]
  def values: Seq[ColumnValue[_]]
  def filter: Filter

  def set(columnValues: ColumnValue[_]*): Update[T]
  def set(t: T): Update[T]

  def where(filter: Filter): Update[T]
}

object Update {
  protected final case class UpdateImpl[T](table: Table[T], values: Seq[ColumnValue[_]] = Seq.empty, filter: Filter = Filter.Empty) extends Update[T] {
    def set(columnValues: ColumnValue[_]*): Update[T] =
      copy(values = columnValues)
    def set(t: T): Update[T] =
      copy(values = table.*.values(t))

    def where(filter: Filter): Update[T] =
      copy(filter = this.filter and filter)
  }

  def apply[T](table: Table[T]) = UpdateImpl(table)
}

sealed trait Delete extends Statement with SideEffect {
  def table: Table[_]
  def filter: Filter

  def where(filter: Filter): Delete
}

object Delete {
  protected final case class DeleteImpl(table: Table[_], filter: Filter = Filter.Empty) extends Delete {
    def where(filter: Filter): Delete =
      copy(filter = this.filter and filter)
  }

  def apply(table: Table[_]) = DeleteImpl(table)
}
