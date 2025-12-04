import os
from app.core import settings
from langchain_google_genai import GoogleGenerativeAIEmbeddings
from langchain_qdrant import QdrantVectorStore
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams
from app.schemas import TopicCreateRequest
from langchain_core.documents import Document
from qdrant_client.models import Filter, FieldCondition, MatchValue
from app.core import settings
if not os.environ.get("GOOGLE_API_KEY"):
  os.environ["GOOGLE_API_KEY"] = settings.GOOGLE_API_KEY

class TopicService:
    def __init__(self):
        embeddings = GoogleGenerativeAIEmbeddings(model="models/gemini-embedding-001")
        self.client = QdrantClient(host=settings.VECTOR_DB_HOST, port=6333)
        collection_name = "topics"
        vector_size = len(embeddings.embed_query("sample text"))
        if not self.client.collection_exists(collection_name):
            self.client.create_collection(
                collection_name=collection_name,
                vectors_config=VectorParams(size=vector_size, distance=Distance.COSINE)
            )

        self.vector_store = QdrantVectorStore(
            client=self.client,
            collection_name=collection_name,
            embedding=embeddings,
        )
    def add_topic(self,topic: TopicCreateRequest):
        doc = Document(page_content= f'{topic.name} {topic.description} {topic.level}',metadata={'id':topic.id,'name':topic.name,'description':topic.description,'topic_type':topic.topic_type, 'level': topic.level })
        self.vector_store.add_documents([doc])
        
    def delete_topic(self,topic_id:str):{
        self.client.delete(
            collection_name="topics",
            points_selector=Filter(
                must=[
                    FieldCondition(
                        key="metadata.id",
                        match=MatchValue(value=topic_id)
                    )
                ]
            )
        )
    }
    def search(self, query:str) ->list:
        results = self.vector_store.similarity_search(query=query,k=5)
        topics = []
        for result in results:
            metadata = result.metadata
            topic = {
                'id':metadata['id'],
                'name':metadata['name'],
                'description':metadata['description'],
                'topic_type':metadata['topic_type'],
                'level': metadata.get('level', "beginner")
            }
            topics.append(topic)
        return topics
            
topic_service = TopicService()